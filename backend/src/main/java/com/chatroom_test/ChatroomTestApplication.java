package com.chatroom_test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ChatroomTestApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChatroomTestApplication.class, args);
	}

}
