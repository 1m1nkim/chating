"use client";

import React, {useEffect, useState} from "react";
import {useRouter} from "next/navigation";

interface Post {
    id: number;
    author: string;
    description: string;
    createdAt: string;
}

export default function PostDetailPage({params}: { params: { postId: string } }) {
    const [post, setPost] = useState<Post | null>(null);
    const [username, setUsername] = useState("");
    const router = useRouter();

    // React.use()를 사용하여 params 언랩
    const postId = React.use(params).postId;  // URL에서 postId 추출

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

    // 게시글 상세 조회
    useEffect(() => {
        const fetchPostDetail = async () => {
            if (!postId) return;

            try {
                const response = await fetch(`http://localhost:8080/api/posts/${postId}`, {
                    credentials: "include",
                });
                if (response.ok) {
                    const data = await response.json();
                    setPost(data);
                } else {
                    alert("게시글을 불러오는 데 실패했습니다.");
                }
            } catch (error) {
                console.error("게시글 상세 조회 중 오류 발생:", error);
                alert("게시글 상세 조회 중 오류가 발생하였습니다.");
            }
        };

        fetchPostDetail();
    }, [postId]);

    // "문의하기" 버튼 클릭 시 채팅방으로 이동
    const handleInquiryClick = () => {
        if (post && post.author !== username) {
            router.push(`/chatroom?receiver=${post.author}`);
        }
    };

    if (!post) return <div>게시글을 불러오는 중...</div>;

    return (
        <div className="min-h-screen bg-gray-100 text-black">
            <header className="bg-white shadow py-4 px-6">
                <h1 className="text-2xl font-bold">게시글 상세보기</h1>
            </header>

            <main className="container mx-auto p-6">
                <div className="bg-white p-6 rounded shadow-md mb-6">
                    <div className="font-bold">{post.author}</div>
                    <div className="text-sm text-gray-500">{new Date(post.createdAt).toLocaleString()}</div>
                    <div className="mt-4">{post.description}</div>
                </div>

                {/* 조건에 따라 문의하기 버튼 표시 */}
                {post.author !== username ? (
                    <button
                        onClick={handleInquiryClick}
                        className="px-4 py-2 bg-yellow-400 text-white rounded hover:bg-yellow-500 transition"
                    >
                        문의하기
                    </button>
                ) : (
                    <div className="text-red-500 mt-4">
                        채팅을 보낼 수 없습니다.
                    </div>
                )}
            </main>
        </div>
    );
}
