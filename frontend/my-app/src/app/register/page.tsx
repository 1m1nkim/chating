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
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ username, password }),
            });
            if (response.ok) {
                router.push("/login");
            } else {
                alert("회원가입 실패. 다른 아이디를 사용해보세요.");
            }
        } catch (error) {
            console.error("회원가입 요청 중 에러 발생:", error);
            alert("회원가입 중 오류가 발생하였습니다.");
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-100">
            <form
                onSubmit={handleRegister}
                className="bg-white p-8 rounded shadow-md w-full max-w-md"
            >
                <h2 className="text-2xl font-bold mb-6 text-center">회원가입</h2>
                <div className="mb-4">
                    <label htmlFor="username" className="block text-gray-700 mb-2">
                        아이디
                    </label>
                    <input
                        type="text"
                        id="username"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        className="w-full p-2 border rounded"
                        required
                    />
                </div>
                <div className="mb-6">
                    <label htmlFor="password" className="block text-gray-700 mb-2">
                        비밀번호
                    </label>
                    <input
                        type="password"
                        id="password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        className="w-full p-2 border rounded"
                        required
                    />
                </div>
                <button
                    type="submit"
                    className="w-full bg-green-500 text-white py-2 rounded hover:bg-green-600 transition"
                >
                    회원가입
                </button>
                <p className="mt-4 text-center">
                    이미 계정이 있으신가요?{" "}
                    <a href="/login" className="text-blue-500 underline">
                        로그인
                    </a>
                </p>
            </form>
        </div>
    );
}
