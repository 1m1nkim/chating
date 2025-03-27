package com.chatroom_test.chat.controller;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.chatroom_test.chat.dto.ChatNotification;
import com.chatroom_test.chat.dto.UnreadCountUpdate;
import com.chatroom_test.chat.entity.ChatMessage;
import com.chatroom_test.chat.service.ChatService;

@Controller
public class ChatController {

	private final SimpMessagingTemplate messagingTemplate;
	private final ChatService chatService;

	@Autowired
	public ChatController(SimpMessagingTemplate messagingTemplate, ChatService chatService) {
		this.messagingTemplate = messagingTemplate;
		this.chatService = chatService;
	}

	// 메시지 전송 처리
	@MessageMapping("/chat.send")
	public void sendMessage(ChatMessage chatMessage) {
		String roomId = chatService.getRoomId(chatMessage.getSender(), chatMessage.getReceiver());
		chatMessage.setRoomId(roomId);
		chatMessage.setTimestamp(LocalDateTime.now());

		// Redis에 메시지 저장 (중복 방지)
		chatService.saveMessage(chatMessage);

		// WebSocket을 통해 메시지 전송
		messagingTemplate.convertAndSend("/topic/chat/" + roomId, chatMessage);

		// 상대방의 읽지 않은 메시지 개수 업데이트
		// 상대방의 마지막 읽은 시간 (예시로 받는 시간 설정, 실제 값은 DB에서 가져올 수 있음)
		long unreadCount = chatService.getUnreadCount(roomId, chatMessage.getReceiver());
		UnreadCountUpdate update = new UnreadCountUpdate(roomId, unreadCount);
		messagingTemplate.convertAndSend("/topic/unreadCount/" + chatMessage.getReceiver(), update);

		// 새 채팅방을 만들 때 알림 전송
		if (chatService.subscribeChatRoom(chatMessage.getSender(), chatMessage.getReceiver()).isNewlyCreated()) {
			String notificationContent = String.format("%s님이 채팅을 시작했습니다.", chatMessage.getSender());
			ChatNotification notification = new ChatNotification(roomId, chatMessage.getSender(), notificationContent);
			messagingTemplate.convertAndSend("/topic/notification/" + chatMessage.getReceiver(), notification);
		}
	}
}

