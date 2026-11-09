package com.example.enrollment_service.integration;

import java.util.UUID;

/**
 * Phát ra sau khi enrollment chuyển sang ACTIVE (enroll hoặc grant).
 * Được xử lý sau khi transaction commit để Progress Service chỉ nhận event của dữ liệu đã lưu.
 */
public record EnrollmentActivatedEvent(UUID enrollmentId, UUID studentId, UUID courseId) {
}
