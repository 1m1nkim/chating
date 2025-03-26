package com.chatroom_test.chat.service;

import com.chatroom_test.chat.entity.ChatMessage;
import com.chatroom_test.chat.entity.ChatRoom;
import com.chatroom_test.chat.repository.ChatMessageRepository;
import com.chatroom_test.chat.repository.ChatRoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ChatService {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    public String getRoomId(String sender, String receiver) {
        if (sender.compareTo(receiver) < 0) {
            return sender + ":" + receiver;
        } else {
            return receiver + ":" + sender;
        }
    }

    public void saveMessage(ChatMessage message) {
        chatMessageRepository.save(message);
    }

    public List<ChatMessage> getMessages(String sender, String receiver) {
        return chatMessageRepository.findBySenderAndReceiver(sender, receiver);
    }

    public ChatRoom subscribeChatRoom(String sender, String receiver) {
        String roomId = getRoomId(sender, receiver);
        Optional<ChatRoom> optionalRoom = chatRoomRepository.findByRoomId(roomId);
        if (optionalRoom.isPresent()) {
            return optionalRoom.get();
        } else {
            ChatRoom newRoom = new ChatRoom(roomId, sender, receiver);
            return chatRoomRepository.save(newRoom);
        }
    }

    public List<ChatRoom> getSubscribedChatRooms(String username) {
        return chatRoomRepository.findByUser1OrUser2(username, username);
    }

    public List<ChatMessage> getMessagesByRoomId(String roomId) {
        return chatMessageRepository.findByRoomIdOrderByIdAsc(roomId);
    }

    // unread 메시지 개수 계산 시 본인 메시지는 제외합니다.
    public long getUnreadCount(String roomId, String username) {
        Optional<ChatRoom> optionalRoom = chatRoomRepository.findByRoomId(roomId);
        if(optionalRoom.isPresent()){
            ChatRoom room = optionalRoom.get();
            LocalDateTime lastRead = null;
            if(username.equals(room.getUser1())){
                lastRead = room.getLastReadAtUser1();
            } else if(username.equals(room.getUser2())){
                lastRead = room.getLastReadAtUser2();
            }
            if(lastRead == null) {
                // 마지막 읽은 시간이 없으면, 전체 메시지 중 본인 메시지를 제외한 개수를 unread로 판단
                return chatMessageRepository.findByRoomIdOrderByIdAsc(roomId).stream()
                        .filter(cm -> !cm.getSender().equals(username))
                        .count();
            } else {
                return chatMessageRepository.countUnreadMessages(roomId, lastRead, username);
            }
        }
        return 0;
    }

    @Transactional
    public ChatRoom markChatRoomAsRead(String roomId, String username) {
        Optional<ChatRoom> optionalRoom = chatRoomRepository.findByRoomId(roomId);
        if(optionalRoom.isPresent()){
            ChatRoom room = optionalRoom.get();
            LocalDateTime now = LocalDateTime.now();

            if(username.equals(room.getUser1())){
                room.setLastReadAtUser1(now);
            } else if(username.equals(room.getUser2())){
                room.setLastReadAtUser2(now);
            }
            return chatRoomRepository.saveAndFlush(room);
        }
        return null;
    }
}
