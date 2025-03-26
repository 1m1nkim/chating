package com.chatroom_test.chat.controller;

import com.chatroom_test.chat.entity.ChatMessage;
import com.chatroom_test.chat.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatRestController {

    private final ChatService chatService;

    @Autowired
    public ChatRestController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/history")
    public List<ChatMessage> getHistory(
            @RequestParam String sender,
            @RequestParam String receiver
    ) {
        return chatService.getMessages(sender, receiver);
    }

    // 새로 추가: roomId로 이력 조회
    @GetMapping("/historyByRoom")
    public List<ChatMessage> getHistoryByRoom(@RequestParam String roomId) {
        return chatService.getMessagesByRoomId(roomId);
    }
}
