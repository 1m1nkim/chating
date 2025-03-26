package com.chatroom_test.chat.controller;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

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
		String roomId = chatService.getRoomId(chatMessage.getSender(), chatMessage.getReceiver());
		chatMessage.setRoomId(roomId);
		chatMessage.setTimestamp(LocalDateTime.now());
		chatService.saveMessage(chatMessage);
		chatService.subscribeChatRoom(chatMessage.getSender(), chatMessage.getReceiver());
		messagingTemplate.convertAndSend("/topic/chat/" + roomId, chatMessage);

		// 여기서 unread 메시지 개수를 계산하고, 수신자에게 전송
		long unreadCount = chatService.getUnreadCount(roomId, chatMessage.getReceiver());
		UnreadCountUpdate update = new UnreadCountUpdate(roomId, unreadCount);
		messagingTemplate.convertAndSend("/topic/unreadCount/" + chatMessage.getReceiver(), update);
	}
}
