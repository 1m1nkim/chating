package com.chatroom_test.chat.dto;

import com.chatroom_test.chat.entity.ChatRoom;

public class ChatRoomResponse {
	private Long id;
	private String roomId;
	private String displayName;
	private long unreadCount;

	public ChatRoomResponse(ChatRoom room, String currentUsername, long unreadCount) {
		this.id = room.getId();
		this.roomId = room.getRoomId();
		if (room.getUser1().equals(room.getUser2())) {
			this.displayName = "나와의 채팅방";
		} else {
			if (currentUsername.equals(room.getUser1())) {
				this.displayName = room.getUser2() + "님과의 채팅방";
			} else if (currentUsername.equals(room.getUser2())) {
				this.displayName = room.getUser1() + "님과의 채팅방";
			} else {
				this.displayName = room.getRoomId();
			}
		}
		this.unreadCount = unreadCount;
	}

	// Getter
	public Long getId() {
		return id;
	}

	public String getRoomId() {
		return roomId;
	}

	public String getDisplayName() {
		return displayName;
	}

	public long getUnreadCount() {
		return unreadCount;
	}
}