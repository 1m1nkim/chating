package com.chatroom_test.user.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chatroom_test.user.repository.UserRepository;

@RestController
@RequestMapping("/api/users")
public class UserController {

	@Autowired
	private UserRepository userRepository;

	@GetMapping("/exists")
	public ResponseEntity<Boolean> userExists(@RequestParam String username) {
		boolean exists = userRepository.findByUsername(username).isPresent();
		return ResponseEntity.ok(exists);
	}
}
