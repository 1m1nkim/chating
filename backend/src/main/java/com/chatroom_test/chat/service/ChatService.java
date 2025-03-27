package com.chatroom_test.chat.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

	// DB와 Redis에 저장된 메시지를 합치고, 중복 메시지를 제거한 후 정렬해서 반환
	public List<ChatMessage> getMessagesByRoomId(String roomId) {
		// DB에서 메시지 조회
		List<ChatMessage> dbMessages = chatMessageRepository.findByRoomIdOrderByIdAsc(roomId);

		// Redis에서 메시지 조회
		List<ChatMessage> redisMessages = redisTemplate.opsForList().range(CHAT_MESSAGE_KEY_PREFIX + roomId, 0, -1);

		// DB와 Redis의 메시지를 합침
		List<ChatMessage> allMessages = new ArrayList<>(dbMessages);
		if (redisMessages != null) {
			allMessages.addAll(redisMessages);
		}

		// 중복 제거: roomId, timestamp, sender, content 기준 (메시지 객체의 equals/hashCode에 의존하지 않고 TreeSet으로 처리)
		List<ChatMessage> distinctMessages = allMessages.stream()
			.collect(Collectors.collectingAndThen(
				Collectors.toCollection(() -> new java.util.TreeSet<>(Comparator
					.comparing((ChatMessage m) -> m.getRoomId() + m.getTimestamp() + m.getSender() + m.getContent()))),
				ArrayList::new));

		// timestamp 기준 오름차순 정렬
		distinctMessages.sort(Comparator.comparing(ChatMessage::getTimestamp));
		return distinctMessages;
	}

	// Redis에 메시지 저장 (메시지 전송 시 한 번만 호출)
	public void saveMessage(ChatMessage message) {
		String key = CHAT_MESSAGE_KEY_PREFIX + message.getRoomId();
		redisTemplate.opsForList().rightPush(key, message);
	}

	// 주기적으로 Redis에 저장된 메시지를 DB로 플러시 (중복 저장 방지를 위해 flush 후 Redis key 삭제)
	@Scheduled(fixedRate = 50000)
	@Transactional
	public void flushMessagesToDB() {
		Set<String> keys = redisTemplate.keys(CHAT_MESSAGE_KEY_PREFIX + "*");
		if (keys != null) {
			for (String key : keys) {
				List<ChatMessage> messages = redisTemplate.opsForList().range(key, 0, -1);
				if (messages != null && !messages.isEmpty()) {
					// DB에 저장 (이미 DB에 있는 메시지와 중복될 가능성이 있으므로,
					// 읽기 시 getMessagesByRoomId에서 deduplication을 수행하여 중복 문제를 해결)
					chatMessageRepository.saveAll(messages);
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

	public List<ChatRoom> getSubscribedChatRooms(String username) {
		return chatRoomRepository.findByUser1OrUser2(username, username);
	}

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
