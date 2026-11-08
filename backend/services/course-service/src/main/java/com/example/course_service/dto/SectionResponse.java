package com.example.course_service.dto;

import java.util.List;
import java.util.UUID;

public record SectionResponse(
		UUID id,
		UUID courseId,
		String title,
		String description,
		Integer position,
		List<LessonResponse> lessons) {
}
