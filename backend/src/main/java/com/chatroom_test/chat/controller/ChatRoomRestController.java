package com.chatroom_test.chat.controller;

import com.chatroom_test.chat.entity.ChatRoom;
import com.chatroom_test.chat.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chatrooms")
public class ChatRoomRestController {

    @Autowired
    private ChatService chatService;

    @GetMapping
    public List<ChatRoom> getChatRooms(@RequestParam String username) {
        return chatService.getSubscribedChatRooms(username);
    }
}
