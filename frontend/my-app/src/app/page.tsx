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

interface ChatNotification {
    roomId: string;
    sender: string;
    contentSnippet: string;
}

const ChatConnection: React.FC = () => {
    const [username, setUsername] = useState("");
    const [receiver, setReceiver] = useState("");
    const [chatRooms, setChatRooms] = useState<ChatRoomResponse[]>([]);
    // 보낸 사람별로 알림을 묶기 위해 Record 자료구조 사용
    // 예: { 'userA': [{roomId, sender, contentSnippet}, ...], 'userB': [...], ... }
    const [notificationsBySender, setNotificationsBySender] = useState<Record<string, ChatNotification[]>>({});
    const router = useRouter();

    // 현재 로그인 사용자 확인
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
                console.error("현재 사용자 정보 확인 실패:", err);
                router.push("/login");
            }
        };
        fetchCurrentUser();
    }, [router]);

    // 채팅방 목록 조회
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
            console.error("채팅방 목록 조회 오류:", err);
        }
    };

    // 채팅방 클릭 시 이동
    const handleChatRoomClick = (roomId: string) => {
        router.push(`/chatroom?roomId=${roomId}`);
    };

    // 알림 클릭 시 해당 채팅방으로 이동
    const handleNotificationClick = (roomId: string) => {
        router.push(`/chatroom?roomId=${roomId}`);
    };

    // 로그아웃
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
            console.error("로그아웃 오류:", error);
            alert("로그아웃 중 오류가 발생했습니다.");
        }
    };

    // WebSocket 연결: unread count + notification
    useEffect(() => {
        if (!username) return;
        const socket = new SockJS("http://localhost:8080/ws-chat");
        const client = Stomp.over(socket);

        client.connect({}, (frame) => {
            console.log("WebSocket Connected: " + frame);

            // 1) unread count 구독
            client.subscribe(`/topic/unreadCount/${username}`, (message) => {
                const update = JSON.parse(message.body);
                setChatRooms((prevRooms) =>
                    prevRooms.map((room) =>
                        room.roomId === update.roomId
                            ? {...room, unreadCount: update.unreadCount}
                            : room
                    )
                );
            });

            // 2) 새로운 메세지 알림 구독
            client.subscribe(`/topic/notification/${username}`, (message) => {
                const notification: ChatNotification = JSON.parse(message.body);
                // notificationsBySender 에 추가
                setNotificationsBySender((prev) => {
                    const sender = notification.sender;
                    const existingList = prev[sender] || [];
                    return {
                        ...prev,
                        [sender]: [notification, ...existingList],
                    };
                });
            });
        });

        return () => {
            if (client) client.disconnect(() => console.log("WebSocket Disconnected"));
        };
    }, [username]);

    // 모든 알림을 초기화 (예: “모두 지우기”)
    const clearNotifications = () => {
        setNotificationsBySender({});
    };

    // 새로운 채팅방 연결
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
                alert("채팅 상대 정보 확인 중 오류가 발생했습니다.");
                return;
            }
            const exists = await existsResponse.json();
            if (!exists) {
                alert("존재하지 않는 사용자입니다.");
                return;
            }
        } catch (error) {
            console.error("채팅 상대 정보 확인 오류:", error);
            alert("채팅 상대 정보 확인 중 오류가 발생했습니다.");
            return;
        }
        router.push(`/chatroom?receiver=${receiver}`);
    };

    return (
        <div className="min-h-screen bg-gray-100 text-black">
            {/* text-black 를 상위 div에 적용하여 모든 텍스트가 검정색으로 보이도록 */}
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
                {/* 알림 영역 */}
                {Object.keys(notificationsBySender).length > 0 && (
                    <div className="bg-blue-100 p-4 rounded shadow mb-6">
                        <div className="flex justify-between items-center mb-2">
                            <h2 className="text-lg font-semibold">새로운 알림</h2>
                            <button
                                onClick={clearNotifications}
                                className="text-sm text-blue-600 underline"
                            >
                                모두 지우기
                            </button>
                        </div>
                        {Object.entries(notificationsBySender).map(([sender, notifs]) => (
                            <div key={sender} className="mb-2">
                                {/* 보낸 사람 구분 */}
                                <div className="font-bold mb-1">{sender} 님 알림</div>
                                <ul className="ml-4 list-disc">
                                    {notifs.map((notif, index) => (
                                        <li
                                            key={index}
                                            className="cursor-pointer hover:underline"
                                            onClick={() => handleNotificationClick(notif.roomId)}
                                        >
                                            {notif.contentSnippet}
                                        </li>
                                    ))}
                                </ul>
                            </div>
                        ))}
                    </div>
                )}

                <div className="bg-white p-6 rounded shadow-md mb-6">
                    <h2 className="text-xl font-semibold mb-4">채팅 시작</h2>
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
