package com.example.course_service.dto;

public record OptionResponse(
		Integer id,
		String content,
		boolean correct,
		Integer position) {
}
