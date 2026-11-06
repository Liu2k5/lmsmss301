package com.example.course_service.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.minio.MinioClient;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

	/**
	 * Khai báo region cố định để sinh presigned URL không cần gọi mạng tới MinIO,
	 * nên service vẫn khởi động được khi MinIO chưa chạy (chỉ upload mới cần MinIO).
	 */
	@Bean
	MinioClient minioClient(MinioProperties properties) {
		return MinioClient.builder()
				.endpoint(properties.endpoint())
				.credentials(properties.accessKey(), properties.secretKey())
				.region(properties.region())
				.build();
	}
}
