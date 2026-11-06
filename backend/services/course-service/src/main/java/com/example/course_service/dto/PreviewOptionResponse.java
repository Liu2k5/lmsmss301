package com.example.course_service.dto;

/** Đáp án trong bản xem trước — cố ý không có cờ {@code correct}. */
public record PreviewOptionResponse(
		Integer id,
		String content,
		Integer position) {
}
