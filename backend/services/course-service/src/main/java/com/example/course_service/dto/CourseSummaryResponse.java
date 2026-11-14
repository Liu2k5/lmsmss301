package com.example.course_service.dto;

import java.util.UUID;
import java.time.LocalDateTime;

import com.example.course_service.entity.CourseStatus;

public record CourseSummaryResponse(
		UUID id,
		UUID instructorId,
		String title,
		String description,
		String thumbnailUrl,
		CourseStatus status,
		UUID categoryId,
		String categoryName,
		LocalDateTime createdAt,
		LocalDateTime updatedAt) {
}
