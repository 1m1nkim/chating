package com.chatroom_test.security;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.chatroom_test.user.service.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Autowired
	private CustomUserDetailsService userDetailsService;

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
		// 먼저 Builder 객체를 받아온 뒤 체이닝을 마치고 build()
		AuthenticationManagerBuilder builder = http.getSharedObject(AuthenticationManagerBuilder.class);
		builder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());

		return builder.build();
	}

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			// H2 콘솔 접근을 위해 frameOptions를 비활성화
			.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()))
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.csrf(csrf -> csrf.disable())    // CSRF 비활성 (필요 시 활성화)

			.authorizeHttpRequests(auth -> auth
				// H2 콘솔 접근 허용
				.requestMatchers("/h2-console/**").permitAll()
				// 회원가입, 로그인, me API는 누구나 접근 가능
				.requestMatchers("/api/auth/**").permitAll()
				// WebSocket, 채팅 REST API 등도 필요하면 열어줌
				.requestMatchers("/ws-chat/**", "/api/chat/**").permitAll()
				// 그 외는 인증 필요
				.anyRequest().authenticated())
			// 폼 로그인 안 쓰고, 완전 REST 방식
			.formLogin(AbstractHttpConfigurer::disable)
			// 세션 정책: 인증 성공 시 세션 생성
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));

		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		// 프론트엔드 도메인 (개발환경 예: http://localhost:3000)
		configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
		configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(Arrays.asList("*"));
		configuration.setExposedHeaders(
			Arrays.asList("Content-Type", "Content-Length, Authorization, X-File-Name, X-File-Size"));
		// 세션 쿠키 포함 허용
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		// 모든 경로에 대해 CORS 설정 적용
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
