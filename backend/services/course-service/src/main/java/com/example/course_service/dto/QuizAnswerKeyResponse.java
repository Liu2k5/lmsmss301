package com.example.course_service.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Đề bài + đáp án đúng để Progress Service chấm điểm (UC-P04).
 * Endpoint trả về DTO này nằm dưới `/internal/**` nên không đi qua Gateway.
 */
public record QuizAnswerKeyResponse(
		UUID quizId,
		UUID courseId,
		UUID lessonId,
		String title,
		BigDecimal passingScore,
		Integer maxAttempts,
		List<QuestionKeyResponse> questions) {
}
