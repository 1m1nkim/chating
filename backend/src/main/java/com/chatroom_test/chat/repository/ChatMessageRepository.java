package com.chatroom_test.chat.repository;

import com.chatroom_test.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySenderAndReceiver(String sender, String receiver);
    List<ChatMessage> findByRoomIdOrderByIdAsc(String roomId);
}
