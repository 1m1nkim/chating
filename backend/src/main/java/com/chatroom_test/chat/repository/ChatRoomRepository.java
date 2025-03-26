package com.chatroom_test.chat.repository;

import com.chatroom_test.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByRoomId(String roomId);
    List<ChatRoom> findByUser1OrUser2(String user1, String user2);
}
