package com.chatroom_test.chat.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chatroom_test.chat.dto.ChatRoomResponse;
import com.chatroom_test.chat.entity.ChatRoom;
import com.chatroom_test.chat.service.ChatService;

@RestController
@RequestMapping("/api/chatrooms")
public class ChatRoomRestController {

	@Autowired
	private ChatService chatService;

	// 기존 채팅방 목록 API와 함께, 읽은 처리 API도 추가
	@PostMapping("/{roomId}/read")
	public ResponseEntity<?> markAsRead(@PathVariable String roomId, @RequestParam String username) {
		ChatRoom updatedRoom = chatService.markChatRoomAsRead(roomId, username);
		if (updatedRoom != null) {
			return ResponseEntity.ok("읽음 처리 완료");
		}
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body("채팅방을 찾을 수 없습니다.");
	}

	@GetMapping
	public List<ChatRoomResponse> getChatRooms(@RequestParam String username) {
		List<ChatRoom> rooms = chatService.getSubscribedChatRooms(username);
		return rooms.stream().map(room -> {
			long unread = chatService.getUnreadCount(room.getRoomId(), username);
			return new ChatRoomResponse(room, username, unread);
		}).collect(Collectors.toList());
	}

	@PostMapping("/leave")
	public ResponseEntity<?> leaveChatRoom(@RequestParam String roomId, @RequestParam String username) {
		ChatRoom updatedRoom = chatService.markChatRoomAsRead(roomId, username);
		if (updatedRoom != null) {
			return ResponseEntity.ok("채팅방 나가기 및 읽음 처리 완료");
		}
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body("채팅방을 찾을 수 없습니다.");
	}

}

