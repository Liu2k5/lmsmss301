package com.example.course_service.dto;

import java.util.UUID;
/** Bài học loại QUIZ dùng cho dropdown chọn bài học khi tạo quiz. */
public record LessonOptionResponse(
		UUID id,
		String title,
		UUID sectionId,
		String sectionTitle,
		UUID courseId,
		String courseTitle,
		UUID quizId,
		String quizTitle) {
}
