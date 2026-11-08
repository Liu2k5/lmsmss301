package com.example.course_service.dto;

import java.util.List;

import com.example.course_service.entity.LessonType;

public record LessonResponse(
		Integer id,
		Integer sectionId,
		String title,
		String description,
		LessonType type,
		Integer position,
		boolean preview,
		/** Id quiz nếu bài học loại QUIZ đã được gắn quiz, ngược lại null. */
		Integer quizId,
		List<MaterialResponse> materials) {
}
