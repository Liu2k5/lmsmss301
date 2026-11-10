package com.example.course_service.dto;

import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record QuizSummaryResponse(
		UUID id,
		UUID lessonId,
		String lessonTitle,
		UUID sectionId,
		String sectionTitle,
		UUID courseId,
		String courseTitle,
		/** Giảng viên phụ trách khoá học (frontend chỉ hiện nút sửa cho chủ khoá học). */
		UUID instructorId,
		String title,
		BigDecimal passingScore,
		Integer maxAttempts,
		int questionCount,
		LocalDateTime updatedAt) {
}
