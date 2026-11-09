package com.example.course_service.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.course_service.dto.QuizAnswerKeyResponse;
import com.example.course_service.service.QuizService;

import lombok.RequiredArgsConstructor;

/**
 * API nội bộ cho các service khác (không đi qua API Gateway).
 * Progress Service dùng để lấy đề + đáp án đúng khi chấm bài nộp (UC-P04).
 */
@RestController
@RequestMapping("/internal/quizzes")
@RequiredArgsConstructor
public class InternalQuizController {

	private final QuizService quizService;

	@GetMapping("/{id}/answer-key")
	public QuizAnswerKeyResponse answerKey(@PathVariable UUID id) {
		return quizService.answerKey(id);
	}
}
