package com.example.course_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Cấu hình MinIO, đọc từ {@code minio.*} trong application.properties (override bằng biến môi trường).
 * Có giá trị mặc định cho MinIO chạy local bằng docker-compose.yml của course-service.
 */
@ConfigurationProperties(prefix = "minio")
public record MinioProperties(
		@DefaultValue("http://localhost:9000") String endpoint,
		@DefaultValue("minioadmin") String accessKey,
		@DefaultValue("minioadmin") String secretKey,
		@DefaultValue("course-materials") String bucket,
		@DefaultValue("us-east-1") String region,
		@DefaultValue("120") int urlExpiryMinutes) {
}
