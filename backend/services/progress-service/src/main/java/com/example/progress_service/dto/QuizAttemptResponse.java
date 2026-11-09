package com.example.progress_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Một lượt làm bài trong lịch sử của học viên. */
public record QuizAttemptResponse(
		UUID id,
		UUID quizId,
		int attemptNumber,
		BigDecimal score,
		boolean passed,
		LocalDateTime submittedAt) {
}
