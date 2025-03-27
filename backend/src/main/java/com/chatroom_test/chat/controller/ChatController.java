package com.chatroom_test.chat.controller;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.chatroom_test.chat.dto.ChatNotification;
import com.chatroom_test.chat.dto.ChatRoomCreationResult;
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

	@MessageMapping("/chat.send")
	public void sendMessage(ChatMessage chatMessage) {
		// 채팅방 ID 결정 (정렬된 sender:receiver 형식)
		String roomId = chatService.getRoomId(chatMessage.getSender(), chatMessage.getReceiver());
		chatMessage.setRoomId(roomId);
		chatMessage.setTimestamp(LocalDateTime.now());

		// 메세지 저장 및 채팅방 구독 업데이트
		chatService.saveMessage(chatMessage);

		ChatRoomCreationResult result = chatService.subscribeChatRoom(chatMessage.getSender(),
			chatMessage.getReceiver());

		// 채팅 메세지를 해당 채팅방으로 전송
		messagingTemplate.convertAndSend("/topic/chat/" + roomId, chatMessage);

		// 읽지 않은 메세지 카운트 계산 후 업데이트
		long unreadCount = chatService.getUnreadCount(roomId, chatMessage.getReceiver());
		UnreadCountUpdate update = new UnreadCountUpdate(roomId, unreadCount);
		messagingTemplate.convertAndSend("/topic/unreadCount/" + chatMessage.getReceiver(), update);

		if (result.isNewlyCreated()) {
			// @님이 메세지를 요청하셨습니다. 형태
			String notificationContent = String.format("%s님이 메세지를 요청하셨습니다.", chatMessage.getSender());
			ChatNotification notification = new ChatNotification(roomId, chatMessage.getSender(), notificationContent);
			messagingTemplate.convertAndSend("/topic/notification/" + chatMessage.getReceiver(), notification);
		}
	}
}
