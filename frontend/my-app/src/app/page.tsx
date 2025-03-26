"use client";

import React, { useState, useEffect } from "react";
import { useRouter } from "next/navigation";

interface ChatRoom {
    id: number;
    roomId: string;
    user1: string;
    user2: string;
}

const ChatConnection: React.FC = () => {
    const [username, setUsername] = useState("");
    const [receiver, setReceiver] = useState("");
    const [chatRooms, setChatRooms] = useState<ChatRoom[]>([]);
    const router = useRouter();

    // 현재 로그인한 사용자 아이디 불러오기
    useEffect(() => {
        const fetchCurrentUser = async () => {
            try {
                const response = await fetch("http://localhost:8080/api/auth/me", {
                    credentials: "include",
                });
                if (response.ok) {
                    const data = await response.text();
                    setUsername(data);
                    fetchChatRooms(data);
                } else {
                    console.warn("User is not logged in or /me returned error");
                }
            } catch (err) {
                console.error("Failed to fetch current user:", err);
            }
        };
        fetchCurrentUser();
    }, []);

    // 구독한 채팅방 목록 불러오기
    const fetchChatRooms = async (user: string) => {
        try {
            const response = await fetch(`http://localhost:8080/api/chatrooms?username=${user}`, {
                credentials: "include",
            });
            if (response.ok) {
                const data = await response.json();
                setChatRooms(data);
            } else {
                console.warn("Failed to fetch chat rooms");
            }
        } catch (err) {
            console.error("Error fetching chat rooms:", err);
        }
    };

    // 새로운 채팅 연결 (받는 사람 입력 후)
    const handleConnect = async () => {
        if (!username) {
            alert("로그인되지 않았거나 username을 불러오지 못했습니다.");
            return;
        }
        if (!receiver.trim()) {
            alert("상대방 아이디를 입력해주세요.");
            return;
        }
        // 새로운 채팅 연결 시 백엔드에서 채팅방 구독(생성) 처리 후 채팅방으로 이동할 수 있습니다.
        // 여기서는 receiver 정보를 쿼리 파라미터로 전달합니다.
        router.push(`/chatroom?receiver=${receiver}`);
    };

    // 구독한 채팅방 클릭 시 해당 채팅방으로 이동 (roomId를 파라미터로 전달)
    const handleChatRoomClick = (roomId: string) => {
        router.push(`/chatroom?roomId=${roomId}`);
    };

    return (
        <div className="p-4 bg-white shadow-md">
            <p className="mb-2">
                <strong>내 아이디:</strong> {username ? username : "(불러오는 중...)"}
            </p>
            <div className="flex space-x-2 mb-2">
                <input
                    type="text"
                    value={receiver}
                    onChange={(e) => setReceiver(e.target.value)}
                    placeholder="상대방 아이디"
                    className="flex-1 p-2 border rounded"
                />
            </div>
            <button
                onClick={handleConnect}
                className="w-full bg-[#FFD700] text-gray-800 p-2 rounded hover:bg-yellow-500 transition mb-4"
            >
                연결하기
            </button>
            <div>
                <h3 className="text-lg font-bold mb-2">구독한 채팅방</h3>
                {chatRooms.length === 0 ? (
                    <p>구독한 채팅방이 없습니다.</p>
                ) : (
                    <ul>
                        {chatRooms.map((room) => (
                            <li key={room.id}>
                                <button
                                    onClick={() => handleChatRoomClick(room.roomId)}
                                    className="text-blue-500 underline"
                                >
                                    {room.roomId}
                                </button>
                            </li>
                        ))}
                    </ul>
                )}
            </div>
        </div>
    );
};

export default function Page() {
    return (
        <div className="flex flex-col items-center justify-center h-screen bg-gray-100">
            <ChatConnection />
        </div>
    );
}
