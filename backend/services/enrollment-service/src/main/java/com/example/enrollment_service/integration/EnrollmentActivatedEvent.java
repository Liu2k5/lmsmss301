package com.example.enrollment_service.integration;

import java.util.UUID;

/**
 * Phát ra sau khi enrollment chuyển sang ACTIVE (enroll hoặc grant).
 * Được xử lý sau khi transaction commit để Progress Service chỉ nhận event của dữ liệu đã lưu.
 *
 * <p>{@code eventId} là id bản ghi {@code enroll_events} của lần kích hoạt này, nên lần đăng ký lại
 * (sau khi huỷ) là một event mới — Progress Service tạo lại tiến độ thay vì bỏ qua vì trùng eventId.
 */
public record EnrollmentActivatedEvent(UUID eventId, UUID enrollmentId, UUID studentId, UUID courseId) {
}
