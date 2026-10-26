package com.example.course_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.course_service.dto.LessonOptionResponse;
import com.example.course_service.dto.QuizRequest;
import com.example.course_service.dto.QuizResponse;
import com.example.course_service.dto.QuizSummaryResponse;
import com.example.course_service.service.QuizService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/quizzes")
@RequiredArgsConstructor
public class QuizController {

	private final QuizService quizService;

	@GetMapping
	public List<QuizSummaryResponse> findAll(@RequestParam(required = false) Integer lessonId) {
		return quizService.findAll(lessonId);
	}

	/** Bài học loại QUIZ còn trống (hoặc đã có quiz) để client chọn khi tạo mới. */
	@GetMapping("/lesson-options")
	public List<LessonOptionResponse> findLessonOptions() {
		return quizService.findLessonOptions();
	}

	@GetMapping("/{id}")
	public QuizResponse findById(@PathVariable Integer id) {
		return quizService.findById(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public QuizResponse create(@Valid @RequestBody QuizRequest request) {
		return quizService.create(request);
	}

	@PutMapping("/{id}")
	public QuizResponse update(@PathVariable Integer id, @Valid @RequestBody QuizRequest request) {
		return quizService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Integer id) {
		quizService.delete(id);
	}
}
