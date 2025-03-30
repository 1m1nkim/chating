package com.chatroom_test.chat.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chat_rooms")
@Getter
@Setter
@NoArgsConstructor
public class ChatRoom {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false)
	private String roomId;

	private String user1;
	private String user2;

	private LocalDateTime lastReadAtUser1;
	private LocalDateTime lastReadAtUser2;

	@OneToMany(mappedBy = "chatRoom")
	@JsonIgnore            //redis의 직렬화를 생략하기 위함
	private List<ChatMessage> messages;

	public ChatRoom(String roomId, String user1, String user2) {
		this.roomId = roomId;
		this.user1 = user1;
		this.user2 = user2;
	}
}
