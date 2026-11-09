package com.example.progress_service.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Đề bài + đáp án đúng của quiz, lấy từ Course Service (API nội bộ). */
public record QuizAnswerKey(
		UUID quizId,
		UUID courseId,
		UUID lessonId,
		String title,
		BigDecimal passingScore,
		Integer maxAttempts,
		List<QuestionKey> questions) {
}
