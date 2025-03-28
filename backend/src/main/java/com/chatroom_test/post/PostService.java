package com.chatroom_test.post;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PostService {

	@Autowired
	private PostRepository postRepository;

	// 게시글 등록
	public Post createPost(String username, String description) {
		Post post = new Post();
		post.setAuthor(username);  // 로그인한 사용자 설정
		post.setDescription(description);
		post.setCreatedAt(LocalDateTime.now());
		return postRepository.save(post);
	}

	// 게시글 수정
	public Post updatePost(Long postId, String description) {
		Post post = postRepository.findById(postId)
			.orElseThrow(() -> new IllegalArgumentException("Post not found"));
		post.setDescription(description);
		return postRepository.save(post);
	}

	// 게시글 삭제
	public void deletePost(Long postId) {
		postRepository.deleteById(postId);
	}

	// 작성자의 모든 게시글 찾기
	public List<Post> getPostsByAuthor(String username) {
		return postRepository.findByAuthor(username);  // findByAuthor 사용
	}

	public List<Post> getPosts() {
		return postRepository.findAll();
	}

	public Post getDetailPost(Long postId) {
		return postRepository.findById(postId).orElseThrow(() -> new IllegalArgumentException("Post not found"));
	}
}
