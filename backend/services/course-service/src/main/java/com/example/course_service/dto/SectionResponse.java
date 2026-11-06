package com.example.course_service.dto;

import java.util.List;

public record SectionResponse(
		Integer id,
		Integer courseId,
		String title,
		String description,
		Integer position,
		List<LessonResponse> lessons) {
}
