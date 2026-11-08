package com.example.course_service.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Bản xem trước dành cho giảng viên: giống những gì học viên sẽ thấy,
 * <b>không</b> chứa thông tin đáp án đúng.
 */
public record QuizPreviewResponse(
		Integer id,
		String title,
		String description,
		BigDecimal passingScore,
		String lessonTitle,
		String sectionTitle,
		String courseTitle,
		List<PreviewQuestionResponse> questions) {
}
