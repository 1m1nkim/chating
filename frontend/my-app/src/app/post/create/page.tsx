"use client";

import React, {useState} from "react";
import {useRouter} from "next/navigation";

export default function CreatePostPage() {
    const [description, setDescription] = useState("");
    const router = useRouter();

    // 게시글 작성 처리
    const handlePostSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!description.trim()) {
            alert("게시글 내용을 입력해주세요.");
            return;
        }

        try {
            const response = await fetch("http://localhost:8080/api/posts/create", {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({description}),
                credentials: "include",
            });

            if (response.ok) {
                const newPost = await response.json();
                // 게시글 작성 후, 게시글 목록 페이지로 리다이렉트
                router.push("/post");
            } else {
                alert("게시글 작성에 실패했습니다.");
            }
        } catch (error) {
            console.error("게시글 작성 중 오류 발생:", error);
            alert("게시글 작성 중 오류가 발생하였습니다.");
        }
    };

    return (
        <div className="min-h-screen bg-gray-100 text-black">
            <header className="bg-white shadow py-4 px-6 flex justify-between items-center">
                <h1 className="text-2xl font-bold">게시글 작성</h1>
            </header>

            <main className="container mx-auto p-6">
                <div className="bg-white p-6 rounded shadow-md mb-6">
                    <h2 className="text-xl font-semibold mb-4">새 게시글 작성</h2>
                    <form onSubmit={handlePostSubmit}>
                        <textarea
                            value={description}
                            onChange={(e) => setDescription(e.target.value)}
                            placeholder="게시글 내용을 입력하세요..."
                            className="w-full p-4 border rounded mb-4"
                            rows={4}
                            required
                        />
                        <button
                            type="submit"
                            className="w-full bg-blue-500 text-white py-2 rounded hover:bg-blue-600 transition"
                        >
                            게시글 작성
                        </button>
                    </form>
                </div>
            </main>
        </div>
    );
}
