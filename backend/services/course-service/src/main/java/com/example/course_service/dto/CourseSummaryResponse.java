package com.example.course_service.dto;

import java.time.LocalDateTime;

import com.example.course_service.entity.CourseStatus;

public record CourseSummaryResponse(
		Integer id,
		Integer instructorId,
		String title,
		String description,
		String thumbnailUrl,
		CourseStatus status,
		long sectionCount,
		long lessonCount,
		LocalDateTime createdAt,
		LocalDateTime updatedAt) {
}
