package com.chatroom_test.chat.dto;

public class UnreadCountUpdate {
	private String roomId;
	private long unreadCount;

	public UnreadCountUpdate() {
	}

	public UnreadCountUpdate(String roomId, long unreadCount) {
		this.roomId = roomId;
		this.unreadCount = unreadCount;
	}

	public String getRoomId() {
		return roomId;
	}

	public void setRoomId(String roomId) {
		this.roomId = roomId;
	}

	public long getUnreadCount() {
		return unreadCount;
	}

	public void setUnreadCount(long unreadCount) {
		this.unreadCount = unreadCount;
	}
}