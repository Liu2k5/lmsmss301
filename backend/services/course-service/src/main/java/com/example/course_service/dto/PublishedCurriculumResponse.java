package com.example.course_service.dto;

import java.util.List;

import com.example.course_service.entity.LessonType;

/**
 * Curriculum của khoá học đã PUBLISHED — contract nội bộ cho Progress Service (UC-P07 tính % hoàn thành).
 * Không trả URL học liệu.
 */
public record PublishedCurriculumResponse(
		Integer courseId,
		String title,
		int totalLessons,
		List<Section> sections) {

	public record Section(Integer id, String title, Integer position, List<Lesson> lessons) {
	}

	public record Lesson(Integer id, String title, LessonType type, Integer position, boolean preview, Integer quizId) {
	}
}
