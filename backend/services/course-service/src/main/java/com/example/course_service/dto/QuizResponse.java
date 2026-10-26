package com.example.course_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record QuizResponse(
		Integer id,
		Integer lessonId,
		String lessonTitle,
		Integer sectionId,
		String sectionTitle,
		Integer courseId,
		String courseTitle,
		String title,
		String description,
		BigDecimal passingScore,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		List<QuestionResponse> questions) {
}
