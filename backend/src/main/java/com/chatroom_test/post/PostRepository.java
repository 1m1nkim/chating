package com.chatroom_test.post;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

	// author 기준으로 게시글 찾기
	List<Post> findByAuthor(String author);  // author 필드 기준으로 게시글 조회

	// 모든 게시글 조회
	List<Post> findAll();  // findPostsAll()을 findAll()로 수정
}
