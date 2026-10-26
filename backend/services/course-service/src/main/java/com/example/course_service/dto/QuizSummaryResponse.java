package com.example.course_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record QuizSummaryResponse(
		Integer id,
		Integer lessonId,
		String lessonTitle,
		Integer courseId,
		String courseTitle,
		String title,
		BigDecimal passingScore,
		int questionCount,
		LocalDateTime updatedAt) {
}
