package com.chatroom_test.chat.entity;

import javax.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_rooms")
public class ChatRoom {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false)
	private String roomId;

	private String user1;
	private String user2;

	// 각 사용자별 마지막 읽은 시각 (없으면 null)
	private LocalDateTime lastReadAtUser1;
	private LocalDateTime lastReadAtUser2;

	public ChatRoom() {
	}

	public ChatRoom(String roomId, String user1, String user2) {
		this.roomId = roomId;
		this.user1 = user1;
		this.user2 = user2;
	}

	// Getter/Setter
	public Long getId() {
		return id;
	}

	public String getRoomId() {
		return roomId;
	}

	public void setRoomId(String roomId) {
		this.roomId = roomId;
	}

	public String getUser1() {
		return user1;
	}

	public void setUser1(String user1) {
		this.user1 = user1;
	}

	public String getUser2() {
		return user2;
	}

	public void setUser2(String user2) {
		this.user2 = user2;
	}

	public LocalDateTime getLastReadAtUser1() {
		return lastReadAtUser1;
	}

	public void setLastReadAtUser1(LocalDateTime lastReadAtUser1) {
		this.lastReadAtUser1 = lastReadAtUser1;
	}

	public LocalDateTime getLastReadAtUser2() {
		return lastReadAtUser2;
	}

	public void setLastReadAtUser2(LocalDateTime lastReadAtUser2) {
		this.lastReadAtUser2 = lastReadAtUser2;
	}
}
