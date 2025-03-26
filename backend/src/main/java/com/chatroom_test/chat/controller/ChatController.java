package com.chatroom_test.chat.controller;

import com.chatroom_test.chat.entity.ChatMessage;
import com.chatroom_test.chat.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

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
        chatService.saveMessage(chatMessage);
        chatService.subscribeChatRoom(chatMessage.getSender(), chatMessage.getReceiver());
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, chatMessage);
    }
}
