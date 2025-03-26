"use client";

import React, { useState } from "react";
import { useRouter } from "next/navigation";

export default function LoginPage() {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const router = useRouter();

    const handleLogin = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            const response = await fetch("http://localhost:8080/api/auth/login", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                credentials: "include", // 세션 쿠키 포함
                body: JSON.stringify({ username, password }),
            });

            if (response.ok) {
                router.push("/");
            } else {
                console.error("로그인 실패", response.status);
            }
        } catch (error) {
            console.error("로그인 요청 중 에러 발생:", error);
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center">
            <form onSubmit={handleLogin} className="bg-white p-6 rounded shadow-md">
                <h2 className="text-xl mb-4">로그인</h2>
                <div className="mb-4">
                    <label htmlFor="username" className="block mb-1">아이디</label>
                    <input
                        type="text"
                        id="username"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        className="border p-2 w-full"
                        required
                    />
                </div>
                <div className="mb-4">
                    <label htmlFor="password" className="block mb-1">비밀번호</label>
                    <input
                        type="password"
                        id="password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        className="border p-2 w-full"
                        required
                    />
                </div>
                <button type="submit" className="bg-blue-500 text-white py-2 px-4 rounded">
                    로그인
                </button>
            </form>
        </div>
    );
}
