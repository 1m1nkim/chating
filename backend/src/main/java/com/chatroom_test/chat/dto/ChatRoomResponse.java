package com.chatroom_test.chat.dto;

import java.time.LocalDateTime;

import com.chatroom_test.chat.entity.ChatRoom;

import lombok.Getter;

@Getter
public class ChatRoomResponse {
	private Long id;
	private String roomId;
	private String displayName;
	private long unreadCount;
	private String lastMessage;
	private LocalDateTime lastMessageTime;

	public ChatRoomResponse(ChatRoom room, String currentUsername, long unreadCount, String lastMessage,
		LocalDateTime lastMessageTime) {
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
		this.lastMessage = lastMessage;
		this.lastMessageTime = lastMessageTime;
	}

}