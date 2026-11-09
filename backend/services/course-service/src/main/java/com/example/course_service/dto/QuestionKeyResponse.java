package com.example.course_service.dto;

import java.util.List;
import java.util.UUID;

import com.example.course_service.entity.QuestionType;

/**
 * Đáp án đúng của một câu hỏi — chỉ dùng nội bộ cho Progress Service chấm bài (UC-P04).
 * Không được trả ra ngoài qua Gateway.
 */
public record QuestionKeyResponse(
		UUID questionId,
		QuestionType type,
		List<UUID> correctOptionIds) {
}
