package com.chatroom_test.chat.dto;

public class ChatNotification {
	private String roomId;
	private String sender;
	private String contentSnippet; // 메세지 내용의 일부 혹은 요약

	public ChatNotification() {
	}

	public ChatNotification(String roomId, String sender, String contentSnippet) {
		this.roomId = roomId;
		this.sender = sender;
		this.contentSnippet = contentSnippet;
	}

	public String getRoomId() {
		return roomId;
	}

	public void setRoomId(String roomId) {
		this.roomId = roomId;
	}

	public String getSender() {
		return sender;
	}

	public void setSender(String sender) {
		this.sender = sender;
	}

	public String getContentSnippet() {
		return contentSnippet;
	}

	public void setContentSnippet(String contentSnippet) {
		this.contentSnippet = contentSnippet;
	}
}
