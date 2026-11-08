package com.example.course_service.controller;

import java.util.List;
import java.util.UUID;

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

import com.example.course_service.dto.DuplicateQuizRequest;
import com.example.course_service.dto.LessonOptionResponse;
import com.example.course_service.dto.QuestionRequest;
import com.example.course_service.dto.QuestionResponse;
import com.example.course_service.dto.QuizPreviewResponse;
import com.example.course_service.dto.QuizRequest;
import com.example.course_service.dto.QuizResponse;
import com.example.course_service.dto.QuizSummaryResponse;
import com.example.course_service.dto.ReorderRequest;
import com.example.course_service.service.QuizService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/quizzes")
@RequiredArgsConstructor
public class QuizController {

	private final QuizService quizService;

	// ============================================================ toàn bộ quiz

	/** Danh sách quiz; có thể lọc theo bài học, khoá học và từ khoá. */
	@GetMapping
	public List<QuizSummaryResponse> findAll(
			@RequestParam(required = false) UUID lessonId,
			@RequestParam(required = false) UUID courseId,
			@RequestParam(required = false) String keyword) {
		return quizService.findAll(lessonId, courseId, keyword);
	}

	/** Bài học loại QUIZ còn trống (hoặc đã có quiz) để client chọn khi tạo mới. */
	@GetMapping("/lesson-options")
	public List<LessonOptionResponse> findLessonOptions() {
		return quizService.findLessonOptions();
	}

	@GetMapping("/{id}")
	public QuizResponse findById(@PathVariable UUID id) {
		return quizService.findById(id);
	}

	/** Bản xem trước như học viên thấy — không chứa đáp án đúng. */
	@GetMapping("/{id}/preview")
	public QuizPreviewResponse preview(@PathVariable UUID id) {
		return quizService.preview(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public QuizResponse create(@Valid @RequestBody QuizRequest request) {
		return quizService.create(request);
	}

	@PutMapping("/{id}")
	public QuizResponse update(@PathVariable UUID id, @Valid @RequestBody QuizRequest request) {
		return quizService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable UUID id) {
		quizService.delete(id);
	}

	/** Nhân bản quiz sang bài học khác chưa có quiz. */
	@PostMapping("/{id}/duplicate")
	@ResponseStatus(HttpStatus.CREATED)
	public QuizResponse duplicate(@PathVariable UUID id, @Valid @RequestBody DuplicateQuizRequest request) {
		return quizService.duplicate(id, request.lessonId());
	}

	// ================================================================ câu hỏi

	@PostMapping("/{quizId}/questions")
	@ResponseStatus(HttpStatus.CREATED)
	public QuestionResponse addQuestion(@PathVariable UUID quizId,
			@Valid @RequestBody QuestionRequest request) {
		return quizService.addQuestion(quizId, request);
	}

	@PutMapping("/{quizId}/questions/{questionId}")
	public QuestionResponse updateQuestion(@PathVariable UUID quizId, @PathVariable UUID questionId,
			@Valid @RequestBody QuestionRequest request) {
		return quizService.updateQuestion(quizId, questionId, request);
	}

	@DeleteMapping("/{quizId}/questions/{questionId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteQuestion(@PathVariable UUID quizId, @PathVariable UUID questionId) {
		quizService.deleteQuestion(quizId, questionId);
	}

	/** Sắp xếp lại câu hỏi: danh sách id đầy đủ theo thứ tự mong muốn. */
	@PutMapping("/{quizId}/questions/order")
	public List<QuestionResponse> reorderQuestions(@PathVariable UUID quizId,
			@Valid @RequestBody ReorderRequest request) {
		return quizService.reorderQuestions(quizId, request.orderedIds());
	}
}
