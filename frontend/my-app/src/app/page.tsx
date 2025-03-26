"use client";

import React, {useEffect, useState} from "react";
import {useRouter} from "next/navigation";
import SockJS from "sockjs-client";
import {Stomp} from "@stomp/stompjs";

interface ChatRoomResponse {
    id: number;
    roomId: string;
    displayName: string;
    unreadCount: number;
}

const ChatConnection: React.FC = () => {
    const [username, setUsername] = useState("");
    const [receiver, setReceiver] = useState("");
    const [chatRooms, setChatRooms] = useState<ChatRoomResponse[]>([]);
    const router = useRouter();

    // 현재 로그인한 사용자 확인 후 채팅방 목록 불러오기
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
                    router.push("/login");
                }
            } catch (err) {
                console.error("현재 사용자 확인 중 오류 발생:", err);
                router.push("/login");
            }
        };
        fetchCurrentUser();
    }, [router]);

    const fetchChatRooms = async (user: string) => {
        try {
            const response = await fetch(
                `http://localhost:8080/api/chatrooms?username=${user}`,
                {credentials: "include"}
            );
            if (response.ok) {
                const data = await response.json();
                setChatRooms(data);
            } else {
                console.warn("채팅방 목록 조회 실패");
            }
        } catch (err) {
            console.error("채팅방 목록 조회 중 오류 발생:", err);
        }
    };

    // 채팅방 클릭 시 해당 채팅방 페이지로 이동
    const handleChatRoomClick = (roomId: string) => {
        router.push(`/chatroom?roomId=${roomId}`);
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

    // 실시간 unread 업데이트를 위한 WebSocket 구독 추가
    useEffect(() => {
        if (!username) return;
        const socket = new SockJS("http://localhost:8080/ws-chat");
        const client = Stomp.over(socket);
        client.connect({}, (frame) => {
            console.log("Unread Count WS Connected: " + frame);
            client.subscribe(`/topic/unreadCount/${username}`, (message) => {
                const update = JSON.parse(message.body);
                // update 객체: { roomId, unreadCount }
                setChatRooms(prevRooms =>
                    prevRooms.map(room =>
                        room.roomId === update.roomId
                            ? {...room, unreadCount: update.unreadCount}
                            : room
                    )
                );
            });
        });
        return () => {
            if (client) client.disconnect(() => console.log("Unread Count WS Disconnected"));
        };
    }, [username]);

    // 입력한 채팅 상대와 채팅방 연결
    const handleConnect = async () => {
        if (!username) {
            alert("로그인이 필요합니다.");
            router.push("/login");
            return;
        }
        if (!receiver.trim()) {
            alert("채팅 상대를 입력해주세요.");
            return;
        }
        try {
            const existsResponse = await fetch(
                `http://localhost:8080/api/users/exists?username=${receiver}`,
                {credentials: "include"}
            );
            if (!existsResponse.ok) {
                alert("상대방 확인 중 오류가 발생했습니다.");
                return;
            }
            const exists = await existsResponse.json();
            if (!exists) {
                alert("해당 사용자를 찾을 수 없습니다.");
                return;
            }
        } catch (error) {
            console.error("상대방 확인 중 오류 발생:", error);
            alert("상대방 확인 중 오류가 발생했습니다.");
            return;
        }
        router.push(`/chatroom?receiver=${receiver}`);
    };

    return (
        <div className="min-h-screen bg-gray-100">
            <header className="bg-white shadow py-4 px-6 flex justify-between items-center">
                <h1 className="text-2xl font-bold">채팅방 목록</h1>
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
            <main className="container mx-auto p-6">
                <div className="bg-white p-6 rounded shadow-md mb-6">
                    <h2 className="text-xl font-semibold mb-4">새 채팅 연결</h2>
                    <div className="flex mb-4">
                        <input
                            type="text"
                            value={receiver}
                            onChange={(e) => setReceiver(e.target.value)}
                            placeholder="채팅 상대 입력"
                            className="flex-1 p-2 border rounded mr-2"
                        />
                        <button
                            onClick={handleConnect}
                            className="px-4 py-2 bg-yellow-500 text-gray-800 rounded hover:bg-yellow-600 transition"
                        >
                            연결
                        </button>
                    </div>
                </div>
                <div className="bg-white p-6 rounded shadow-md">
                    <h2 className="text-xl font-semibold mb-4">채팅방 목록</h2>
                    {chatRooms.length === 0 ? (
                        <p className="text-gray-600">채팅방이 없습니다.</p>
                    ) : (
                        <ul>
                            {chatRooms.map((room) => (
                                <li key={room.id} className="mb-2 flex justify-between items-center">
                                    <button
                                        onClick={() => handleChatRoomClick(room.roomId)}
                                        className="text-blue-500 underline"
                                    >
                                        {room.displayName}
                                    </button>
                                    {room.unreadCount > 0 && (
                                        <span className="bg-red-500 text-white rounded-full px-2 py-1 text-xs">
                                            {room.unreadCount}
                                        </span>
                                    )}
                                </li>
                            ))}
                        </ul>
                    )}
                </div>
            </main>
        </div>
    );
};

export default function Page() {
    return <ChatConnection/>;
}
