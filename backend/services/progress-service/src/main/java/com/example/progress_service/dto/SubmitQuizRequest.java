package com.example.progress_service.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Học viên nộp bài (UC-P04). Câu hỏi không gửi lên được coi là bỏ trống (sai). */
public record SubmitQuizRequest(
		@NotNull(message = "Student ID is required") UUID studentId,
		@NotNull(message = "Answers are required") @Valid List<SubmittedAnswer> answers) {

	public record SubmittedAnswer(
			@NotNull(message = "Question ID is required") UUID questionId,
			List<UUID> optionIds) {
	}
}
