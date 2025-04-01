package com.chatroom_test.chat.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chatroom_test.chat.dto.ChatNotification;
import com.chatroom_test.chat.dto.ChatRoomCreationResult;
import com.chatroom_test.chat.dto.UnreadCountUpdate;
import com.chatroom_test.chat.entity.ChatMessage;
import com.chatroom_test.chat.entity.ChatRoom;
import com.chatroom_test.chat.repository.ChatMessageRepository;
import com.chatroom_test.chat.repository.ChatRoomRepository;

@Service
public class ChatService {

	@Autowired
	private ChatMessageRepository chatMessageRepository;

	@Autowired
	private ChatRoomRepository chatRoomRepository;

	@Autowired
	private RedisTemplate<String, ChatMessage> redisTemplate;

	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	private static final String CHAT_MESSAGE_KEY_PREFIX = "chat:messages:";

	public ChatRoom getChatRoom(String sender, String receiver) {
		String roomId = getRoomId(sender, receiver);
		return chatRoomRepository.findByRoomId(roomId)
			.orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));
	}

	public List<ChatMessage> getMessagesByRoomId(String roomId) {
		// Redis에서 메시지 조회
		List<ChatMessage> redisMessages = redisTemplate.opsForList().range(CHAT_MESSAGE_KEY_PREFIX + roomId, 0, -1);

		// Redis에 메시지가 없으면 DB에서 조회 후 Redis에 저장
		if (redisMessages == null || redisMessages.isEmpty()) {
			List<ChatMessage> dbMessages = chatMessageRepository.findByChatRoomRoomIdOrderByIdAsc(roomId);
			if (dbMessages != null && !dbMessages.isEmpty()) {
				redisTemplate.opsForList().rightPushAll(CHAT_MESSAGE_KEY_PREFIX + roomId, dbMessages);
			}
			return dbMessages;
		}

		if (redisMessages == null) {
			redisMessages = new ArrayList<>();
		}

		return redisMessages;
	}

	// 메시지를 Redis에 저장 (메시지 전송 시 한 번만 호출)
	public void saveMessage(ChatMessage message) {
		String key = CHAT_MESSAGE_KEY_PREFIX + message.getChatRoom().getRoomId();
		redisTemplate.opsForList().rightPush(key, message);
	}

	// Redis에 저장된 메시지를 주기적으로 DB에 플러시 (중복 방지 위해 Redis key 삭제)
	@Scheduled(fixedRate = 30000)
	@Transactional
	public void flushMessagesToDB() {
		Set<String> keys = redisTemplate.keys(CHAT_MESSAGE_KEY_PREFIX + "*");
		if (keys != null) {
			for (String key : keys) {
				List<ChatMessage> messages = redisTemplate.opsForList().range(key, 0, -1);
				if (messages != null && !messages.isEmpty()) {
					for (ChatMessage message : messages) {
						if (!chatMessageRepository.existsByChatRoomRoomIdAndTimestamp(message.getChatRoom().getRoomId(),
							message.getTimestamp())) {
							chatMessageRepository.save(message);
						}
					}
					redisTemplate.delete(key);
					System.out.println("Flushed " + messages.size() + " messages from Redis to DB for key: " + key);
				}
			}
		}
	}

	// 두 사용자의 알파벳 순서를 기준으로 채팅방 아이디 생성
	public String getRoomId(String sender, String receiver) {
		return (sender.compareTo(receiver) < 0) ? sender + ":" + receiver : receiver + ":" + sender;
	}

	// 채팅방에 가입된 사용자 목록 조회
	public List<ChatRoom> getSubscribedChatRooms(String username) {
		return chatRoomRepository.findByClientOrExpert(username, username);
	}

	// 채팅방 구독 및 생성 (신규 채팅방일 경우 true 반환)
	public ChatRoomCreationResult subscribeChatRoom(String sender, String receiver) {
		String roomId = getRoomId(sender, receiver);
		return chatRoomRepository.findByRoomId(roomId)
			.map(room -> new ChatRoomCreationResult(room, false))
			.orElseGet(() -> {
				ChatRoom newRoom = new ChatRoom(roomId, sender, receiver);
				ChatRoom savedRoom = chatRoomRepository.save(newRoom);
				return new ChatRoomCreationResult(savedRoom, true);
			});
	}

	// unread count 계산: 채팅방의 마지막 읽은 시간 이후에 상대방이 보낸 메시지만 카운트
	public long getUnreadCount(String roomId, String username) {
		ChatRoom room = chatRoomRepository.findByRoomId(roomId).orElse(null);
		if (room == null)
			return 0;
		LocalDateTime lastRead = null;
		if (username.equals(room.getClient())) {
			lastRead = room.getLastReadAtUser1();
		} else if (username.equals(room.getExpert())) {
			lastRead = room.getLastReadAtUser2();
		}
		List<ChatMessage> allMessages = getMessagesByRoomId(roomId);
		final LocalDateTime finalLastRead = lastRead;
		return allMessages.stream()
			.filter(cm -> !cm.getSender().equals(username))
			.filter(cm -> finalLastRead == null || cm.getTimestamp().isAfter(finalLastRead))
			.count();
	}

	// 채팅방 읽음 처리: 사용자가 채팅방에 들어갈 때 lastReadAt 갱신
	@Transactional
	public ChatRoom markChatRoomAsRead(String roomId, String username) {
		return chatRoomRepository.findByRoomId(roomId).map(room -> {
			LocalDateTime now = LocalDateTime.now();
			if (username.equals(room.getClient())) {
				room.setLastReadAtUser1(now);
			} else if (username.equals(room.getExpert())) {
				room.setLastReadAtUser2(now);
			}
			return chatRoomRepository.saveAndFlush(room);
		}).orElse(null);
	}

	public ChatMessage getLastMessage(String roomId) {
		List<ChatMessage> messages = getMessagesByRoomId(roomId);
		if (messages == null || messages.isEmpty()) {
			return null;
		}
		return messages.get(messages.size() - 1);
	}

	// 메시지 전송: 채팅방 구독/생성, 메시지 저장, WebSocket 전송, unread count 업데이트, 신규 채팅방 알림 전송
	public void sendMessage(ChatMessage chatMessage) {
		ChatRoomCreationResult roomResult = subscribeChatRoom(chatMessage.getSender(), chatMessage.getReceiver());
		ChatRoom chatRoom = roomResult.getChatRoom();

		chatMessage.setChatRoom(chatRoom);
		chatMessage.setTimestamp(LocalDateTime.now());

		saveMessage(chatMessage);
		messagingTemplate.convertAndSend("/topic/chat/" + chatRoom.getRoomId(), chatMessage);

		long unreadCount = getUnreadCount(chatRoom.getRoomId(), chatMessage.getReceiver());
		UnreadCountUpdate update = new UnreadCountUpdate(chatRoom.getRoomId(), unreadCount);
		messagingTemplate.convertAndSend("/topic/unreadCount/" + chatMessage.getReceiver(), update);

		if (roomResult.isNewlyCreated()) {
			String notificationContent = String.format("%s님이 채팅을 시작했습니다.", chatMessage.getSender());
			ChatNotification notification = new ChatNotification(chatRoom.getRoomId(), chatMessage.getSender(),
				notificationContent);
			messagingTemplate.convertAndSend("/topic/notification/" + chatMessage.getReceiver(), notification);
		}
	}
}
