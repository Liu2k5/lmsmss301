package com.example.course_service.dto;

import java.util.List;
import java.util.UUID;

import com.example.course_service.entity.QuestionType;

public record QuestionResponse(
		UUID id,
		String content,
		QuestionType type,
		Integer position,
		List<OptionResponse> options) {
}
