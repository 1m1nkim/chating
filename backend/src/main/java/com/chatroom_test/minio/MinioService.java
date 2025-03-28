package com.chatroom_test.minio;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;

@Service
public class MinioService {

	private final MinioClient minioClient;
	private final String bucketName;

	public MinioService(@Value("${minio.url}") String minioUrl,
		@Value("${minio.accessKey}") String minioAccessKey,
		@Value("${minio.secretKey}") String minioSecretKey,
		@Value("${minio.bucketName}") String bucketName) {
		this.minioClient = MinioClient.builder()
			.endpoint(minioUrl)
			.credentials(minioAccessKey, minioSecretKey)
			.build();
		this.bucketName = bucketName;
	}

	public String uploadFile(MultipartFile file) throws IOException, Exception {
		String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

		boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
		if (!found) {
			minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
		}

		minioClient.putObject(
			PutObjectArgs.builder()
				.bucket(bucketName)
				.object(fileName)
				.stream(file.getInputStream(), file.getSize(), -1)
				.contentType(file.getContentType())
				.build()
		);

		return fileName;
	}

	public String getFileUrl(String fileName) throws Exception {
		return minioClient.getPresignedObjectUrl(
			GetPresignedObjectUrlArgs.builder()
				.bucket(bucketName)
				.object(fileName)
				.method(Method.GET)
				.expiry(60, TimeUnit.MINUTES)
				.build()
		);
	}
}
