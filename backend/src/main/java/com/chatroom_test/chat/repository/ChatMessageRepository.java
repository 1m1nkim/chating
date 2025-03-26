package com.chatroom_test.chat.repository;

import com.chatroom_test.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySenderAndReceiver(String sender, String receiver);
    List<ChatMessage> findByRoomIdOrderByIdAsc(String roomId);

    // 본인이 보낸 메시지는 unread 카운트에서 제외하도록 조건 추가
    @Query("select count(cm) from ChatMessage cm where cm.roomId = :roomId and cm.timestamp > :lastRead and cm.sender <> :username")
    long countUnreadMessages(@Param("roomId") String roomId,
                             @Param("lastRead") LocalDateTime lastRead,
                             @Param("username") String username);

}
