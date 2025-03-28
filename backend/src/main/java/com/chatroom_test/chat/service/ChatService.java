package com.chatroom_test.chat.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chatroom_test.chat.dto.ChatRoomCreationResult;
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

	private static final String CHAT_MESSAGE_KEY_PREFIX = "chat:messages:";

	public List<ChatMessage> getMessagesByRoomId(String roomId) {
		// Redis에서 메시지 조회
		List<ChatMessage> redisMessages = redisTemplate.opsForList().range(CHAT_MESSAGE_KEY_PREFIX + roomId, 0, -1);

		// Redis에 메시지가 없으면 DB에서 조회하여 Redis에 저장
		if (redisMessages == null || redisMessages.isEmpty()) {
			// DB에서 메시지 조회
			List<ChatMessage> dbMessages = chatMessageRepository.findByRoomIdOrderByIdAsc(roomId);
			// Redis에 저장
			if (dbMessages != null && !dbMessages.isEmpty()) {
				redisTemplate.opsForList().rightPushAll(CHAT_MESSAGE_KEY_PREFIX + roomId, dbMessages);
			}
			return dbMessages; // Redis에 저장 후 DB 메시지를 반환
		}

		// Redis에서 반환된 값이 null일 경우 빈 리스트로 처리
		if (redisMessages == null) {
			redisMessages = new ArrayList<>();
		}

		return redisMessages;
	}

	// 메시지를 Redis에 저장 (메시지 전송 시 한 번만 호출)
	public void saveMessage(ChatMessage message) {
		String key = CHAT_MESSAGE_KEY_PREFIX + message.getRoomId();
		redisTemplate.opsForList().rightPush(key, message);
	}

	// 주기적으로 Redis에 저장된 메시지를 DB로 플러시 (중복 저장 방지를 위해 flush 후 Redis key 삭제)
	@Scheduled(fixedRate = 30000)  // 30초마다 실행
	@Transactional
	public void flushMessagesToDB() {
		Set<String> keys = redisTemplate.keys(CHAT_MESSAGE_KEY_PREFIX + "*");
		if (keys != null) {
			for (String key : keys) {
				List<ChatMessage> messages = redisTemplate.opsForList().range(key, 0, -1);
				if (messages != null && !messages.isEmpty()) {
					// DB에 저장 (이미 DB에 있는 메시지와 중복될 가능성이 있으므로,
					// 읽기 시 getMessagesByRoomId에서 deduplication을 수행하여 중복 문제를 해결)
					for (ChatMessage message : messages) {
						if (!chatMessageRepository.existsByRoomIdAndTimestamp(message.getRoomId(),
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
		return chatRoomRepository.findByUser1OrUser2(username, username);
	}

	// 채팅방 구독 및 생성
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

	// unread count를 계산할 때, 채팅방의 마지막 읽은 시간 이후에 수신된 상대방 메시지만 카운트
	public long getUnreadCount(String roomId, String username) {
		ChatRoom room = chatRoomRepository.findByRoomId(roomId).orElse(null);
		if (room == null)
			return 0;
		LocalDateTime lastRead = null;
		if (username.equals(room.getUser1())) {
			lastRead = room.getLastReadAtUser1();
		} else if (username.equals(room.getUser2())) {
			lastRead = room.getLastReadAtUser2();
		}
		// DB와 Redis의 메시지를 모두 고려하여 중복 제거된 메시지 리스트를 가져옴
		List<ChatMessage> allMessages = getMessagesByRoomId(roomId);
		// 상대방이 보낸 메시지 중, lastRead가 null이거나 lastRead 이후의 메시지를 unread로 간주
		final LocalDateTime finalLastRead = lastRead;
		return allMessages.stream()
			.filter(cm -> !cm.getSender().equals(username))
			.filter(cm -> finalLastRead == null || cm.getTimestamp().isAfter(finalLastRead))
			.count();
	}

	// 채팅방 읽음 처리: 사용자가 채팅방에 들어갈 때 해당 사용자의 lastReadAt 갱신
	@Transactional
	public ChatRoom markChatRoomAsRead(String roomId, String username) {
		return chatRoomRepository.findByRoomId(roomId).map(room -> {
			LocalDateTime now = LocalDateTime.now();
			if (username.equals(room.getUser1())) {
				room.setLastReadAtUser1(now);
			} else if (username.equals(room.getUser2())) {
				room.setLastReadAtUser2(now);
			}
			return chatRoomRepository.saveAndFlush(room);
		}).orElse(null);
	}
}