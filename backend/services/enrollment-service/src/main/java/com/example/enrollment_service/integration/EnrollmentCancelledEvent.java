package com.example.enrollment_service.integration;

import java.util.UUID;

/**
 * Phát ra sau khi học viên tự huỷ đăng ký khoá học (UC-E02).
 * Progress Service dùng event này để lưu trữ (archive) tiến độ học tập (UC-P11).
 */
public record EnrollmentCancelledEvent(UUID eventId, UUID studentId, UUID courseId) {
}
