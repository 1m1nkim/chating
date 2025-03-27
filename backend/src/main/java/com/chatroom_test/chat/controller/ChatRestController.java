package com.chatroom_test.chat.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chatroom_test.chat.entity.ChatMessage;
import com.chatroom_test.chat.service.ChatService;

@RestController
@RequestMapping("/api/chat")
public class ChatRestController {

	private final ChatService chatService;

	@Autowired
	public ChatRestController(ChatService chatService) {
		this.chatService = chatService;
	}

	// 새로 추가: roomId로 이력 조회
	@GetMapping("/historyByRoom")
	public List<ChatMessage> getHistoryByRoom(@RequestParam String roomId) {
		return chatService.getMessagesByRoomId(roomId);
	}
}
