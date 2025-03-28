"use client";

import React, {useEffect, useState} from "react";
import {useRouter} from "next/navigation";

interface Post {
    id: number;
    author: string;
    description: string;
    createdAt: string;
}

export default function PostPage() {
    const [posts, setPosts] = useState<Post[]>([]);
    const [description, setDescription] = useState("");
    const [loading, setLoading] = useState(false);
    const router = useRouter();

    // 게시글 목록을 가져오는 함수
    useEffect(() => {
        const fetchPosts = async () => {
            setLoading(true);
            try {
                const response = await fetch("http://localhost:8080/api/posts", {
                    credentials: "include",
                });
                if (response.ok) {
                    const data = await response.json();
                    setPosts(data);
                } else {
                    alert("게시글을 불러오는 데 실패했습니다.");
                }
            } catch (error) {
                console.error("게시글을 가져오는 중 오류 발생:", error);
                alert("게시글을 가져오는 중 오류가 발생하였습니다.");
            } finally {
                setLoading(false);
            }
        };

        fetchPosts();
    }, []);

    // 게시글 작성 함수
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
                setPosts([newPost, ...posts]);  // 새 게시글을 리스트의 맨 위에 추가
                setDescription("");  // 입력 필드 초기화
            } else {
                alert("게시글 작성에 실패했습니다.");
            }
        } catch (error) {
            console.error("게시글 작성 중 오류 발생:", error);
            alert("게시글 작성 중 오류가 발생하였습니다.");
        }
    };

    // 게시글 상세보기 페이지로 이동하는 함수
    const handleViewPost = (postId: number) => {
        router.push(`/post/${postId}`);  // 해당 게시글의 ID를 가지고 상세보기 페이지로 이동
    };

    return (
        <div className="min-h-screen bg-gray-100 text-black">
            <header className="bg-white shadow py-4 px-6 flex justify-between items-center">
                <h1 className="text-2xl font-bold">게시판</h1>
                <button
                    onClick={() => router.push("/post/create")}  // 게시글 작성 페이지로 이동
                    className="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 transition"
                >
                    게시글 작성
                </button>
            </header>

            <main className="container mx-auto p-6">
                <div className="bg-white p-6 rounded shadow-md mb-6">
                    <h2 className="text-xl font-semibold mb-4">게시글 목록</h2>
                    {loading ? (
                        <div>Loading posts...</div>
                    ) : (
                        <div>
                            {posts.length === 0 ? (
                                <p>게시글이 없습니다.</p>
                            ) : (
                                <ul>
                                    {posts.map((post) => (
                                        <li key={post.id} className="mb-4 p-4 border rounded-lg shadow-sm">
                                            <div className="font-bold">{post.author}</div>
                                            <div>{post.description}</div>
                                            <div
                                                className="text-sm text-gray-500">{new Date(post.createdAt).toLocaleString()}</div>
                                            <button
                                                onClick={() => handleViewPost(post.id)}  // 상세보기 버튼 클릭 시 해당 게시글로 이동
                                                className="text-blue-500 underline mt-2"
                                            >
                                                자세히 보기
                                            </button>
                                        </li>
                                    ))}
                                </ul>
                            )}
                        </div>
                    )}
                </div>
            </main>
        </div>
    );
}
