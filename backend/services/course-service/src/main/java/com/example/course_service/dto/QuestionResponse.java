package com.example.course_service.dto;

import java.util.List;

import com.example.course_service.entity.QuestionType;

public record QuestionResponse(
		Integer id,
		String content,
		QuestionType type,
		Integer position,
		List<OptionResponse> options) {
}
