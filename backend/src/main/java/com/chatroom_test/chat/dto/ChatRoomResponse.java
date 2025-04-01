package com.chatroom_test.chat.dto;

import java.time.LocalDateTime;

import com.chatroom_test.chat.entity.ChatRoom;

public record ChatRoomResponse(
	String roomId,
	String displayName,
	long unreadCount,
	String lastMessage,
	LocalDateTime lastMessageTime
) {
	public ChatRoomResponse(ChatRoom room, String currentUsername, long unreadCount, String lastMessage,
		LocalDateTime lastMessageTime) {
		this(room.getRoomId(), computeDisplayName(room, currentUsername), unreadCount, lastMessage, lastMessageTime);
	}

	private static String computeDisplayName(ChatRoom room, String currentUsername) {
		if (room.getClient().equals(room.getExpert())) {
			return "나와의 채팅방";
		} else {
			if (currentUsername.equals(room.getClient())) {
				return room.getExpert() + "님과의 채팅방";
			} else if (currentUsername.equals(room.getExpert())) {
				return room.getClient() + "님과의 채팅방";
			} else {
				return room.getRoomId();
			}
		}
	}
}