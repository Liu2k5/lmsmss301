package com.example.course_service.dto;

import java.util.List;
import java.util.UUID;

import com.example.course_service.entity.LessonType;

/**
 * Curriculum của khoá học đã PUBLISHED — contract nội bộ cho Progress Service (UC-P07 tính % hoàn thành).
 * Không trả URL học liệu.
 */
public record PublishedCurriculumResponse(
		UUID courseId,
		String title,
		int totalLessons,
		List<Section> sections) {

	public record Section(UUID id, String title, Integer position, List<Lesson> lessons) {
	}

	public record Lesson(UUID id, String title, LessonType type, Integer position, boolean preview, UUID quizId) {
	}
}
