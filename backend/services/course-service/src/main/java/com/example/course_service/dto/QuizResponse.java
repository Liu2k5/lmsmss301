package com.example.course_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record QuizResponse(
		UUID id,
		UUID lessonId,
		String lessonTitle,
		UUID sectionId,
		String sectionTitle,
		UUID courseId,
		String courseTitle,
		String title,
		String description,
		BigDecimal passingScore,
		Integer maxAttempts,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		List<QuestionResponse> questions) {
}
