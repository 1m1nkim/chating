package com.chatroom_test.chat.dto;

import com.chatroom_test.chat.entity.ChatRoom;

public class ChatRoomCreationResult {
	private final ChatRoom chatRoom;
	private final boolean newlyCreated;

	public ChatRoomCreationResult(ChatRoom chatRoom, boolean newlyCreated) {
		this.chatRoom = chatRoom;
		this.newlyCreated = newlyCreated;
	}

	public ChatRoom getChatRoom() {
		return chatRoom;
	}

	public boolean isNewlyCreated() {
		return newlyCreated;
	}
}
