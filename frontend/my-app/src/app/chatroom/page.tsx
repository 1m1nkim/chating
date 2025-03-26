"use client";

import React, { useState, useEffect, useRef } from "react";
import * as SockJS from "sockjs-client";
import { Stomp } from "@stomp/stompjs";
import { Send, User } from "lucide-react";
import { useSearchParams } from "next/navigation";

interface ChatMessage {
    sender: string;
    receiver: string;
    content: string;
    timestamp?: string;
}

const ChatRoom: React.FC = () => {
    const [username, setUsername] = useState("");
    const [receiver, setReceiver] = useState("");
    const [content, setContent] = useState("");
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [stompClient, setStompClient] = useState<any>(null);
    const [roomId, setRoomId] = useState("");
    const [isConnected, setIsConnected] = useState(false);

    const messagesEndRef = useRef<HTMLDivElement | null>(null);
    const searchParams = useSearchParams();

    // 현재 로그인한 사용자 아이디를 불러옵니다.
    useEffect(() => {
        const fetchCurrentUser = async () => {
            try {
                const response = await fetch("http://localhost:8080/api/auth/me", {
                    credentials: "include",
                });
                if (response.ok) {
                    const data = await response.text();
                    setUsername(data);
                } else {
                    console.warn("User is not logged in or /me returned error");
                }
            } catch (err) {
                console.error("Failed to fetch current user:", err);
            }
        };
        fetchCurrentUser();
    }, []);

    // URL 쿼리 파라미터에서 roomId 또는 receiver를 가져와 설정합니다.
    useEffect(() => {
        if (!username) return;
        const roomParam = searchParams.get("roomId");
        const receiverParam = searchParams.get("receiver");

        if (roomParam) {
            setRoomId(roomParam);
            // roomParam 형식: "userA:userB"
            const parts = roomParam.split(":");
            if (parts.length === 2) {
                // 현재 사용자가 어느쪽에 있는지 확인 후, 반대편을 receiver로 설정
                if (parts[0] === username) {
                    setReceiver(parts[1]);
                } else {
                    setReceiver(parts[0]);
                }
            }
        } else if (receiverParam) {
            setReceiver(receiverParam);
            // username과 receiver를 비교해 roomId 생성
            const computedRoomId = username < receiverParam ? `${username}:${receiverParam}` : `${receiverParam}:${username}`;
            setRoomId(computedRoomId);
        }
    }, [username, searchParams]);

    // 메시지 업데이트 시 스크롤 하단으로 이동
    useEffect(() => {
        if (messagesEndRef.current) {
            messagesEndRef.current.scrollIntoView({ behavior: "smooth" });
        }
    }, [messages]);

    // 채팅 이력을 불러옵니다.
    const loadHistory = async () => {
        if (!roomId) return;
        try {
            const response = await fetch(`http://localhost:8080/api/chat/historyByRoom?roomId=${roomId}`, {
                credentials: "include",
            });
            if (response.ok) {
                const data: ChatMessage[] = await response.json();
                setMessages(data);
            } else {
                console.error("Failed to fetch history", response.status);
            }
        } catch (error) {
            console.error("Error fetching history:", error);
        }
    };

    // 웹소켓 연결
    useEffect(() => {
        if (!username || !receiver || !roomId) return;
        loadHistory();

        const socket = new SockJS("http://localhost:8080/ws-chat");
        const client = Stomp.over(socket);
        client.connect({}, (frame: any) => {
            console.log("Connected: " + frame);
            setIsConnected(true);

            // 채팅방 토픽 구독
            client.subscribe(`/topic/chat/${roomId}`, (message: any) => {
                const msg: ChatMessage = JSON.parse(message.body);
                setMessages((prev) => [...prev, msg]);
            });
        });
        setStompClient(client);

        // 컴포넌트 언마운트 시 연결 해제
        return () => {
            if (client) client.disconnect(() => console.log("Disconnected"));
        };
    }, [username, receiver, roomId]);

    // 메시지 전송
    const sendMessage = () => {
        if (!stompClient) {
            alert("먼저 연결해주세요");
            return;
        }
        if (!content.trim()) {
            alert("메시지를 입력해주세요");
            return;
        }

        const chatMessage: ChatMessage = {
            sender: username,
            receiver: receiver,
            content: content,
            timestamp: new Date().toISOString(),
        };

        stompClient.send("/app/chat.send", {}, JSON.stringify(chatMessage));
        setContent("");
    };

    // 엔터키로 메시지 전송
    const handleKeyPress = (e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.key === "Enter") {
            sendMessage();
        }
    };

    return (
        <div className="flex flex-col h-screen max-w-md mx-auto bg-gray-100">
            {/* 헤더 영역 */}
            <div className="bg-[#FFD700] p-4 flex items-center shadow-md">
                <User className="mr-3 text-gray-700" />
                <div>
                    <h2 className="text-lg font-bold text-gray-800">
                        {receiver ? receiver : "채팅 상대"}
                    </h2>
                    <p className="text-sm text-gray-600">{isConnected ? "온라인" : "오프라인"}</p>
                    <p className="text-xs text-gray-500">채팅방 ID: {roomId}</p>
                </div>
            </div>

            {/* 메시지 목록 */}
            <div className="flex-1 overflow-y-auto p-4 space-y-3">
                {messages.map((msg, idx) => (
                    <div key={idx} className={`flex ${msg.sender === username ? "justify-end" : "justify-start"}`}>
                        <div
                            className={`max-w-[70%] p-3 rounded-lg ${
                                msg.sender === username ? "bg-[#FFD700] text-gray-800" : "bg-white text-gray-800 shadow-md"
                            }`}
                        >
                            {msg.sender !== username && <div className="font-bold text-sm mb-1">{msg.sender}</div>}
                            <div>{msg.content}</div>
                        </div>
                    </div>
                ))}
                <div ref={messagesEndRef} />
            </div>

            {/* 메시지 입력창 */}
            {isConnected && (
                <div className="bg-white p-4 border-t flex items-center space-x-2">
                    <input
                        type="text"
                        value={content}
                        onChange={(e) => setContent(e.target.value)}
                        onKeyPress={handleKeyPress}
                        placeholder="메시지를 입력하세요"
                        className="flex-1 p-2 border rounded-full"
                    />
                    <button onClick={sendMessage} className="bg-[#FFD700] p-2 rounded-full hover:bg-yellow-500 transition">
                        <Send className="text-gray-800" size={20} />
                    </button>
                </div>
            )}
        </div>
    );
};

export default ChatRoom;
