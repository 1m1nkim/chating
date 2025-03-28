package com.chatroom_test.post;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
public class PostController {

	@Autowired
	private PostService postService;

	// 게시글 등록 (로그인된 사용자 정보를 @AuthenticationPrincipal로 가져옴)
	@PostMapping("/create")
	public Post createPost(@RequestBody PostRequest postRequest, @AuthenticationPrincipal UserDetails userDetails) {
		return postService.createPost(userDetails.getUsername(), postRequest.getDescription());
	}

	// 게시글 수정
	@PutMapping("/update/{postId}")
	public Post updatePost(@PathVariable Long postId, @RequestBody String description) {
		return postService.updatePost(postId, description);
	}

	// 게시글 삭제
	@DeleteMapping("/delete/{postId}")
	public void deletePost(@PathVariable Long postId) {
		postService.deletePost(postId);
	}

	// 특정 사용자의 게시글 조회
	@GetMapping("/user/{username}")
	public List<Post> getPostsByUsername(@PathVariable String username) {
		return postService.getPostsByAuthor(username);
	}

	@GetMapping
	public List<Post> getPosts() {
		return postService.getPosts();
	}

	@GetMapping("/{postId}")
	public Post getDetailPost(@PathVariable Long postId) {
		return postService.getDetailPost(postId);
	}
}
