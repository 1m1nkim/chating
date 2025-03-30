"use client";

import React, {useEffect, useRef, useState} from "react";
import * as SockJS from "sockjs-client";
import {Stomp} from "@stomp/stompjs";
import {Paperclip, Send} from "lucide-react";
import {useRouter, useSearchParams} from "next/navigation";

interface ChatMessage {
    sender: string;
    receiver: string;
    content: string;
    timestamp?: string;
    fileUrl?: string;
}

const ChatRoom: React.FC = () => {
    const [username, setUsername] = useState("");
    const [receiver, setReceiver] = useState("");
    const [content, setContent] = useState("");
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [stompClient, setStompClient] = useState<any>(null);
    const [roomId, setRoomId] = useState("");
    const [isConnected, setIsConnected] = useState(false);
    const [file, setFile] = useState<File | null>(null);

    const messagesEndRef = useRef<HTMLDivElement | null>(null);
    const fileInputRef = useRef<HTMLInputElement>(null);
    const searchParams = useSearchParams();
    const router = useRouter();

    // 현재 사용자 확인
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
                    router.push("/login");
                }
            } catch (err) {
                console.error("현재 사용자 확인 중 오류 발생:", err);
                router.push("/login");
            }
        };
        fetchCurrentUser();
    }, [router]);

    // 채팅방 ID 및 상대방 설정
    useEffect(() => {
        if (!username) return;
        const roomParam = searchParams.get("roomId");
        const receiverParam = searchParams.get("receiver");

        if (roomParam) {
            setRoomId(roomParam);
            const parts = roomParam.split(":");
            if (parts.length === 2) {
                setReceiver(parts[0] === username ? parts[1] : parts[0]);
            }
        } else if (receiverParam) {
            setReceiver(receiverParam);
            const computedRoomId =
                username < receiverParam
                    ? `${username}:${receiverParam}`
                    : `${receiverParam}:${username}`;
            setRoomId(computedRoomId);
        }
    }, [username, searchParams]);

    // 채팅 기록 로드
    const loadHistory = async () => {
        if (!roomId) return;
        try {
            const response = await fetch(
                `http://localhost:8080/api/chat/historyByRoom?roomId=${roomId}`,
                {credentials: "include"}
            );
            if (response.ok) {
                const data: ChatMessage[] = await response.json();
                setMessages(data);
            } else {
                console.error("채팅 기록 조회 실패", response.status);
            }
        } catch (error) {
            console.error("채팅 기록 조회 중 오류 발생:", error);
        }
    };

    // WebSocket 연결 설정
    useEffect(() => {
        if (!username || !receiver || !roomId) return;
        loadHistory();
        const socket = new SockJS("http://localhost:8080/ws-chat");
        const client = Stomp.over(socket);
        client.connect({}, (frame: any) => {
            console.log("Connected: " + frame);
            setIsConnected(true);
            client.subscribe(`/topic/chat/${roomId}`, (message: any) => {
                const msg: ChatMessage = JSON.parse(message.body);
                setMessages((prev) => [...prev, msg]);
            });
        });
        setStompClient(client);

        return () => {
            if (client) client.disconnect(() => console.log("Disconnected"));
        };
    }, [username, receiver, roomId]);

    // ★ 읽음 처리 로직 추가 ★
    // 채팅방에 입장하면 해당 채팅방의 읽음 처리 API를 호출하여,
    // 사용자의 읽지 않은 메시지를 초기화합니다.
    useEffect(() => {
        if (roomId && username) {
            fetch(`http://localhost:8080/api/chatrooms/${roomId}/read?username=${username}`, {
                method: "POST",
                credentials: "include",
            })
                .then(response => {
                    if (!response.ok) {
                        console.warn("읽음 처리 응답 상태:", response.status);
                        return null;
                    }
                    return response.text();
                })
                .then(data => {
                    if (data) {
                        console.log("Mark as read response:", data);
                    }
                })
                .catch(error => {
                    console.error("Error marking chat room as read:", error);
                });
        }
    }, [roomId, username]);
    // ★ 여기까지 읽음 처리 로직 추가 ★

    // 메시지 전송
    const sendMessage = () => {
        if (!stompClient) {
            alert("메시지 전송이 불가능합니다.");
            return;
        }
        if (!content.trim() && !file) {
            alert("메시지를 입력하거나 파일을 선택해주세요.");
            return;
        }

        if (file) {
            uploadFile();
        } else {
            sendTextMessage(content);
        }
    };

    // 텍스트 메시지 전송
    const sendTextMessage = (textContent: string) => {
        const chatMessage: ChatMessage = {
            sender: username,
            receiver: receiver,
            content: textContent,
            timestamp: new Date().toISOString(),
        };
        stompClient.send("/app/chat.send", {}, JSON.stringify(chatMessage));
        setContent("");
    };

    // 파일 업로드 처리
    const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        if (event.target.files && event.target.files[0]) {
            setFile(event.target.files[0]);
        }
    };

    // 파일 업로드 API 호출
    const uploadFile = async () => {
        if (!file) return;

        const formData = new FormData();
        formData.append("file", file);

        try {
            const response = await fetch("http://localhost:8080/api/files/upload", {
                method: "POST",
                body: formData,
                credentials: "include",
            });

            if (response.ok) {
                const data = await response.json();
                sendFileMessage(data.fileUrl); // 파일 URL 전송
            } else {
                console.error("파일 업로드 실패");
            }
        } catch (error) {
            console.error("파일 업로드 중 오류 발생:", error);
        }

        setFile(null);
        if (fileInputRef.current) fileInputRef.current.value = "";
    };

    const sendFileMessage = (fileUrl: string) => {
        if (!stompClient) return;

        const chatMessage: ChatMessage = {
            sender: username,
            receiver: receiver,
            content: "이미지 전송", // 원하는 문구 설정 가능
            timestamp: new Date().toISOString(),
            fileUrl, // 반드시 전달
        };

        stompClient.send("/app/chat.send", {}, JSON.stringify(chatMessage));
    };

    return (
        <div className="flex flex-col h-screen bg-gray-100">
            <header className="bg-white shadow py-4 px-6 flex justify-between items-center">
                <h1 className="text-2xl font-bold">채팅방</h1>
                <span>현재 사용자: {username}</span>
            </header>

            <main className="flex-1 flex flex-col max-w-md mx-auto">
                {/* 메시지 목록 */}
                <div className="flex-1 overflow-y-auto p-4 space-y-3">
                    {messages.map((msg, idx) => (
                        <div key={idx} className={`flex ${msg.sender === username ? "justify-end" : "justify-start"}`}>
                            <div
                                className={`max-w-[70%] p-3 rounded-lg ${
                                    msg.sender === username ? "bg-yellow-400 text-gray-800" : "bg-white text-gray-800 shadow-md"
                                }`}
                            >
                                {msg.sender !== username && (
                                    <div className="font-bold text-sm mb-1">{msg.sender}</div>
                                )}
                                {msg.fileUrl ? (
                                    <img src={msg.fileUrl} alt="Uploaded file"
                                         className="max-w-full h-auto rounded-lg"/>
                                ) : (
                                    <div>{msg.content}</div>
                                )}
                            </div>
                        </div>
                    ))}
                    <div ref={messagesEndRef}/>
                </div>

                {/* 입력 필드 */}
                <div className="bg-white p-4 border-t flex items-center space-x-2">
                    <input
                        type="text"
                        value={content}
                        onChange={(e) => setContent(e.target.value)}
                        placeholder="메시지를 입력하세요"
                        className="flex-1 p-2 border rounded-full"
                    />
                    <input type="file" onChange={handleFileChange} ref={fileInputRef} className="hidden"/>
                    <button
                        onClick={() => fileInputRef.current?.click()}
                        className="bg-gray-200 p-2 rounded-full hover:bg-gray-300 transition"
                    >
                        <Paperclip size={20}/>
                    </button>
                    <button onClick={sendMessage}
                            className="bg-yellow-400 p-2 rounded-full hover:bg-yellow-500 transition">
                        <Send size={20}/>
                    </button>
                </div>
            </main>
        </div>
    );
};

export default ChatRoom;
