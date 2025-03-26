"use client";

import React, {useState, useEffect, useRef} from "react";
import * as SockJS from "sockjs-client";
import {Stomp} from "@stomp/stompjs";
import {Send, User} from "lucide-react";
import {useSearchParams, useRouter} from "next/navigation";

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
    const router = useRouter();

    // 현재 로그인한 사용자 확인
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

    // URL 매개변수를 통해 roomId 혹은 receiver 설정
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

    // 채팅 기록 불러오기
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

    // 브라우저 창이 포커스될 때 읽음 처리 API 호출
    useEffect(() => {
        const handleFocus = () => {
            if (username && roomId) {
                fetch(`http://localhost:8080/api/chatrooms/${roomId}/read?username=${username}`, {
                    method: "POST",
                    credentials: "include",
                }).catch(error => console.error("읽음 처리 중 오류 발생:", error));
            }
        };

        window.addEventListener("focus", handleFocus);
        return () => {
            window.removeEventListener("focus", handleFocus);
        };
    }, [username, roomId]);

    // 컴포넌트 언마운트 시 읽음 처리 API 호출 (즉시 업데이트)
    useEffect(() => {
        return () => {
            if (username && roomId) {
                fetch(`http://localhost:8080/api/chatrooms/${roomId}/read?username=${username}`, {
                    method: "POST",
                    credentials: "include",
                }).catch(error => console.error("읽음 처리 중 오류 발생:", error));
            }
        };
    }, [username, roomId]);

    // 스크롤 자동 이동
    useEffect(() => {
        if (messagesEndRef.current) {
            messagesEndRef.current.scrollIntoView({behavior: "smooth"});
        }
    }, [messages]);

    // WebSocket 연결 및 구독
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

    // 메시지 전송
    const sendMessage = () => {
        if (!stompClient) {
            alert("메시지 전송이 불가능합니다.");
            return;
        }
        if (!content.trim()) {
            alert("메시지를 입력해주세요.");
            return;
        }
        // 메시지 전송 시점에 정확한 시간을 기록
        const chatMessage: ChatMessage = {
            sender: username,
            receiver: receiver,
            content: content,
            timestamp: new Date().toISOString(),
        };
        stompClient.send("/app/chat.send", {}, JSON.stringify(chatMessage));
        setContent("");
    };

    const handleKeyPress = (e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.key === "Enter") sendMessage();
    };

    // 채팅방 나가기 시 읽음 처리 API 호출
    const handleBack = () => {
        fetch(`http://localhost:8080/api/chatrooms/leave?roomId=${roomId}&username=${username}`, {
            method: "POST",
            credentials: "include",
        })
            .then(response => {
                if (response.ok) {
                    router.push("/");
                }
            })
            .catch(error => {
                console.error("채팅방 나가기 중 오류 발생:", error);
            });
    };

    const handleLogout = async () => {
        try {
            const response = await fetch("http://localhost:8080/api/auth/logout", {
                method: "POST",
                credentials: "include",
            });
            if (response.ok) {
                router.push("/login");
            } else {
                alert("로그아웃 실패");
            }
        } catch (error) {
            console.error("로그아웃 중 오류 발생:", error);
            alert("로그아웃 중 오류가 발생했습니다.");
        }
    };

    return (
        <div className="flex flex-col h-screen bg-gray-100">
            <header className="bg-white shadow py-4 px-6 flex justify-between items-center">
                <div className="flex items-center">
                    <button onClick={handleBack} className="mr-4 text-blue-500 underline">
                        뒤로가기
                    </button>
                    <h1 className="text-2xl font-bold">채팅방</h1>
                </div>
                <div>
                    <span className="mr-4">현재 사용자: {username}</span>
                    <button
                        onClick={handleLogout}
                        className="px-4 py-2 bg-red-500 text-white rounded hover:bg-red-600 transition"
                    >
                        로그아웃
                    </button>
                </div>
            </header>
            <main className="flex-1 flex flex-col max-w-md mx-auto">
                <div className="bg-yellow-400 p-4 flex items-center shadow-md">
                    <User className="mr-3 text-gray-700"/>
                    <div>
                        <h2 className="text-lg font-bold text-gray-800">
                            {receiver ? receiver : "채팅 상대"}
                        </h2>
                        <p className="text-sm text-gray-600">
                            {isConnected ? "연결됨" : "연결 대기중"}
                        </p>
                        <p className="text-xs text-gray-500">채팅방 ID: {roomId}</p>
                    </div>
                </div>
                <div className="flex-1 overflow-y-auto p-4 space-y-3">
                    {messages.map((msg, idx) => (
                        <div
                            key={idx}
                            className={`flex ${msg.sender === username ? "justify-end" : "justify-start"}`}
                        >
                            <div
                                className={`max-w-[70%] p-3 rounded-lg ${
                                    msg.sender === username
                                        ? "bg-yellow-400 text-gray-800"
                                        : "bg-white text-gray-800 shadow-md"
                                }`}
                            >
                                {msg.sender !== username && (
                                    <div className="font-bold text-sm mb-1">{msg.sender}</div>
                                )}
                                <div>{msg.content}</div>
                            </div>
                        </div>
                    ))}
                    <div ref={messagesEndRef}/>
                </div>
                <div className="bg-white p-4 border-t flex items-center space-x-2">
                    <input
                        type="text"
                        value={content}
                        onChange={(e) => setContent(e.target.value)}
                        onKeyPress={handleKeyPress}
                        placeholder="메시지를 입력하세요"
                        className="flex-1 p-2 border rounded-full"
                    />
                    <button
                        onClick={sendMessage}
                        className="bg-yellow-400 p-2 rounded-full hover:bg-yellow-500 transition"
                    >
                        <Send className="text-gray-800" size={20}/>
                    </button>
                </div>
            </main>
        </div>
    );
};

export default ChatRoom;
