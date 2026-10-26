package com.example.course_service.dto;

/** Bài học loại QUIZ dùng cho dropdown chọn bài học khi tạo quiz. */
public record LessonOptionResponse(
		Integer id,
		String title,
		Integer sectionId,
		String sectionTitle,
		Integer courseId,
		String courseTitle,
		Integer quizId,
		String quizTitle) {
}
