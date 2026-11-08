package com.example.course_service.dto;

import java.util.UUID;
/** Đáp án trong bản xem trước — cố ý không có cờ {@code correct}. */
public record PreviewOptionResponse(
		UUID id,
		String content,
		Integer position) {
}
