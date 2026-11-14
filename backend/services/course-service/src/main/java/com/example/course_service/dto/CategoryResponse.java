package com.example.course_service.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/** Thông tin category kèm số khoá học đang thuộc category đó. */
public record CategoryResponse(
		UUID id,
		String name,
		String description,
		long courseCount,
		LocalDateTime createdAt,
		LocalDateTime updatedAt) {
}
