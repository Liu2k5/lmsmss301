package com.example.progress_service.dto;

/** Trạng thái đăng ký khoá học, lấy từ Enrollment Service (UC-E04). */
public record EnrollmentStatus(boolean enrolled, String status, boolean accessGranted) {
}
