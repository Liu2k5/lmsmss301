package com.example.progress_service.client;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.example.progress_service.dto.EnrollmentStatus;

import lombok.extern.slf4j.Slf4j;

/**
 * Kiểm tra quyền truy cập khoá học với Enrollment Service (endpoint nội bộ `/enrollments/check`,
 * không đi qua Gateway). Dùng khi học viên làm quiz mà Progress Service chưa có bản ghi tiến độ
 * (ví dụ event StudentEnrolled bị lỡ) để không chặn oan người đã đăng ký.
 */
@Slf4j
@Component
public class EnrollmentClient {

    private final RestClient rest;

    public EnrollmentClient(
            @Value("${app.services.enrollment.url:http://localhost:8083}") String enrollmentServiceUrl) {
        this.rest = RestClient.builder().baseUrl(enrollmentServiceUrl).build();
    }

    public Optional<EnrollmentStatus> check(UUID studentId, UUID courseId) {
        try {
            EnrollmentStatus status = rest.get()
                    .uri("/api/v1/enrollments/check?studentId={studentId}&courseId={courseId}",
                            studentId, courseId)
                    .retrieve()
                    .body(EnrollmentStatus.class);
            return Optional.ofNullable(status);
        }
        catch (RuntimeException ex) {
            log.warn("Không kiểm tra được đăng ký của {} / {}: {}", studentId, courseId, ex.getMessage());
            return Optional.empty();
        }
    }
}
