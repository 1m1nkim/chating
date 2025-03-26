"use client";

import React, { useState } from "react";
import { useRouter } from "next/navigation";

export default function RegisterPage() {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const router = useRouter();

    const handleRegister = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            const response = await fetch("http://localhost:8080/api/auth/register", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({ username, password }),
            });

            if (response.ok) {
                // 회원가입 성공
                router.push("/login"); // Next.js의 로그인 페이지로 이동
            } else {
                // 회원가입 실패
                console.error("회원가입 실패", response.status);
            }
        } catch (error) {
            console.error("회원가입 요청 중 에러 발생:", error);
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center">
            <form onSubmit={handleRegister} className="bg-white p-6 rounded shadow-md">
                <h2 className="text-xl mb-4">회원가입</h2>
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
                <button type="submit" className="bg-green-500 text-white py-2 px-4 rounded">
                    회원가입
                </button>
            </form>
        </div>
    );
}
