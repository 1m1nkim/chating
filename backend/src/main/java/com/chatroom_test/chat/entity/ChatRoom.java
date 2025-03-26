package com.chatroom_test.chat.entity;

import javax.persistence.*;

@Entity
@Table(name = "chat_rooms")
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 예: "alice:bob" 형식의 roomId
    @Column(unique = true, nullable = false)
    private String roomId;

    private String user1;
    private String user2;

    public ChatRoom() {}

    public ChatRoom(String roomId, String user1, String user2) {
        this.roomId = roomId;
        this.user1 = user1;
        this.user2 = user2;
    }

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
}
