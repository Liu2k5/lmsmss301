package com.example.enrollment_service.integration;

import java.util.UUID;

/**
 * Phát ra sau khi enrollment bị huỷ hoặc thu hồi quyền (cancel hoặc revoke),
 * để Progress Service lưu trữ (archive) tiến độ học tập của học viên.
 */
public record EnrollmentDeactivatedEvent(UUID eventId, UUID studentId, UUID courseId) {
}
