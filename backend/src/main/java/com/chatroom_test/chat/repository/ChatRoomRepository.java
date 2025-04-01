package com.chatroom_test.chat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chatroom_test.chat.entity.ChatRoom;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
	Optional<ChatRoom> findByRoomId(String roomId);

	List<ChatRoom> findByClientOrExpert(String client, String expert);

}
