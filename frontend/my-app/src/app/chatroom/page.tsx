'use client';

import React, {useRef, useState} from "react";
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

    // 파일 업로드 처리
    const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        if (event.target.files && event.target.files[0]) {
            setFile(event.target.files[0]);
        }
    };

    const uploadFile = async () => {
        if (!file) return;

        const formData = new FormData();
        formData.append('file', file);

        try {
            const response = await fetch('http://localhost:8080/api/files/upload', {
                method: 'POST',
                body: formData,
                credentials: 'include',
            });

            if (response.ok) {
                const data = await response.json();
                sendFileMessage(data.fileUrl); // 파일 URL 전송
            } else {
                console.error('파일 업로드 실패');
            }
        } catch (error) {
            console.error('파일 업로드 중 오류 발생:', error);
        }

        setFile(null);
        if (fileInputRef.current) fileInputRef.current.value = '';
    };

    // 파일 메시지 전송
    const sendFileMessage = (fileUrl: string) => {
        if (!stompClient) return;

        const chatMessage: ChatMessage = {
            sender: username,
            receiver: receiver,
            content: `Uploaded a file`,
            timestamp: new Date().toISOString(),
            fileUrl,
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
                                className={`max-w-[70%] p-3 rounded-lg ${msg.sender === username ? "bg-yellow-400 text-gray-800" : "bg-white text-gray-800 shadow-md"}`}>
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
                    <button onClick={() => fileInputRef.current?.click()}
                            className="bg-gray-200 p-2 rounded-full hover:bg-gray-300 transition">
                        <Paperclip size={20}/>
                    </button>
                    <button onClick={() => uploadFile()}
                            className="bg-yellow-400 p-2 rounded-full hover:bg-yellow-500 transition">
                        <Send size={20}/>
                    </button>
                </div>
            </main>
        </div>
    );
};

export default ChatRoom;
