package com.example.course_service.dto;

import java.util.UUID;
public record OptionResponse(
		UUID id,
		String content,
		boolean correct,
		Integer position) {
}
