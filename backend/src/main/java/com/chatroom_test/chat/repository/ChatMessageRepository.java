package com.chatroom_test.chat.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chatroom_test.chat.entity.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

	List<ChatMessage> findBySenderAndReceiver(String sender, String receiver);

	// findByChatRoomRoomIdOrderByIdAsc: 채팅방(roomId) 기준으로 오름차순 정렬
	List<ChatMessage> findByChatRoomRoomIdOrderByIdAsc(String roomId);

	// 채팅방의 마지막 읽은 시간 이후에 수신된 상대방 메시지 개수를 계산
	@Query("select count(cm) from ChatMessage cm where cm.chatRoom.roomId = :roomId and cm.timestamp > :lastRead and cm.sender <> :username")
	long countUnreadMessages(@Param("roomId") String roomId,
		@Param("lastRead") LocalDateTime lastRead,
		@Param("username") String username);

	// 중복 메시지 저장 여부 확인
	boolean existsByChatRoomRoomIdAndTimestamp(@Param("roomId") String roomId,
		@Param("timestamp") LocalDateTime timestamp);
}
