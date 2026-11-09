package com.example.progress_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Kết quả một lần nộp bài: điểm, đạt/trượt, lượt thứ mấy và còn được làm nữa không. */
public record QuizSubmitResult(
		UUID quizId,
		UUID courseId,
		int attemptNumber,
		Integer maxAttempts,
		BigDecimal score,
		BigDecimal passingScore,
		boolean passed,
		int correctCount,
		int totalQuestions,
		boolean lessonCompleted,
		int remainingAttempts) {
}
