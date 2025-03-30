package com.chatroom_test.chat.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import com.chatroom_test.chat.entity.ChatMessage;
import com.chatroom_test.chat.service.ChatService;

@Controller
public class ChatController {

	private final ChatService chatService;

	@Autowired
	public ChatController(ChatService chatService) {
		this.chatService = chatService;
	}

	// 메시지 전송 처리
	@MessageMapping("/chat.send")
	public void sendMessage(ChatMessage chatMessage) {
		chatService.sendMessage(chatMessage);
	}
}
