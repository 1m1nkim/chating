package com.chatroom_test.minio;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

	private final MinioService minioService;

	@Autowired
	public FileUploadController(MinioService minioService) {
		this.minioService = minioService;
	}

	@PostMapping("/upload")
	public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
		try {
			// 파일 업로드
			String fileName = minioService.uploadFile(file);
			String fileUrl = minioService.getFileUrl(fileName);

			// 성공 시 파일 URL 반환
			return ResponseEntity.ok(Map.of(
				"status", "success",
				"fileName", fileName,
				"fileUrl", fileUrl
			));
		} catch (Exception e) {
			// 실패 시 에러 메시지 반환
			return ResponseEntity.status(500).body(Map.of(
				"status", "error",
				"message", "파일 업로드 실패: " + e.getMessage()
			));
		}
	}

	@GetMapping("/url/{fileName}")
	public ResponseEntity<String> getFileUrl(@PathVariable String fileName) {
		try {
			String url = minioService.getFileUrl(fileName);
			return ResponseEntity.ok(url);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(500).body("Error getting file URL: " + e.getMessage());
		}
	}
}
