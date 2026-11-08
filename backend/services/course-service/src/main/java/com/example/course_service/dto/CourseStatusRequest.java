package com.example.course_service.dto;

import com.example.course_service.entity.CourseStatus;

import jakarta.validation.constraints.NotNull;

/** Chuyển trạng thái khoá học: xuất bản, ẩn, lưu trữ, khôi phục (UC-C21 → UC-C24). */
public record CourseStatusRequest(
		@NotNull(message = "Trạng thái mới không được để trống")
		CourseStatus status) {
}
