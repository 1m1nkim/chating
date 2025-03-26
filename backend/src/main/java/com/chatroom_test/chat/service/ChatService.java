package com.chatroom_test.chat.service;

import com.chatroom_test.chat.entity.ChatMessage;
import com.chatroom_test.chat.entity.ChatRoom;
import com.chatroom_test.chat.repository.ChatMessageRepository;
import com.chatroom_test.chat.repository.ChatRoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatService {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    // 채팅방 ID 생성 (두 사용자 이름을 정렬하여 생성)
    public String getRoomId(String sender, String receiver) {
        if (sender.compareTo(receiver) < 0) {
            return sender + ":" + receiver;
        } else {
            return receiver + ":" + sender;
        }
    }

    public void saveMessage(ChatMessage message) {
        chatMessageRepository.save(message);
    }

    public List<ChatMessage> getMessages(String sender, String receiver) {
        return chatMessageRepository.findBySenderAndReceiver(sender, receiver);
    }

    // 채팅방 구독(없으면 생성) 기능
    public ChatRoom subscribeChatRoom(String sender, String receiver) {
        String roomId = getRoomId(sender, receiver);
        Optional<ChatRoom> optionalRoom = chatRoomRepository.findByRoomId(roomId);
        if (optionalRoom.isPresent()) {
            return optionalRoom.get();
        } else {
            ChatRoom newRoom = new ChatRoom(roomId, sender, receiver);
            return chatRoomRepository.save(newRoom);
        }
    }

    // 특정 사용자가 참여한 채팅방 목록 조회
    public List<ChatRoom> getSubscribedChatRooms(String username) {
        return chatRoomRepository.findByUser1OrUser2(username, username);
    }

    public List<ChatMessage> getMessagesByRoomId(String roomId) {
        return chatMessageRepository.findByRoomIdOrderByIdAsc(roomId);
    }
}
