package com.example.course_service.dto;

import java.util.List;
import java.util.UUID;

import com.example.course_service.entity.LessonType;

public record LessonResponse(
		UUID id,
		UUID sectionId,
		String title,
		String description,
		LessonType type,
		Integer position,
		boolean preview,
		/**
		 * true = người xem chưa được xem nội dung bài này (chưa đăng ký khoá học và bài không cho xem thử):
		 * danh sách học liệu vẫn có tên / loại nhưng {@code url} = null.
		 */
		boolean locked,
		/** Id quiz nếu bài học loại QUIZ đã được gắn quiz, ngược lại null. */
		UUID quizId,
		List<MaterialResponse> materials) {
}
