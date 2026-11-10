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
import com.example.course_service.security.LoginUser;
import com.example.course_service.service.QuizService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Quiz (UC-C14…C19). Quyền theo vai trò được chặn trong SecurityConfig:
 * <ul>
 *   <li>Danh sách quiz, bản xem trước (không có đáp án): mọi người đã đăng nhập</li>
 *   <li>Chi tiết quiz (có đáp án đúng), danh sách bài học để gắn quiz, tạo / sửa / xoá: INSTRUCTOR và ADMIN;
 *       khi sửa, service kiểm tra thêm có phải giảng viên phụ trách khoá học không</li>
 * </ul>
 */
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

	/** Chi tiết quiz kèm đáp án đúng: chỉ người soạn quiz được xem. */
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
		LoginUser loginUser = LoginUser.current();
		return quizService.create(loginUser, request);
	}

	@PutMapping("/{id}")
	public QuizResponse update(@PathVariable UUID id,
			@Valid @RequestBody QuizRequest request) {
		LoginUser loginUser = LoginUser.current();
		return quizService.update(loginUser, id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable UUID id) {
		LoginUser loginUser = LoginUser.current();
		quizService.delete(loginUser, id);
	}

	/** Nhân bản quiz sang bài học khác chưa có quiz. */
	@PostMapping("/{id}/duplicate")
	@ResponseStatus(HttpStatus.CREATED)
	public QuizResponse duplicate(@PathVariable UUID id,
			@Valid @RequestBody DuplicateQuizRequest request) {
		LoginUser loginUser = LoginUser.current();
		return quizService.duplicate(loginUser, id, request.lessonId());
	}

	// ================================================================ câu hỏi

	@PostMapping("/{quizId}/questions")
	@ResponseStatus(HttpStatus.CREATED)
	public QuestionResponse addQuestion(@PathVariable UUID quizId,
			@Valid @RequestBody QuestionRequest request) {
		LoginUser loginUser = LoginUser.current();
		return quizService.addQuestion(loginUser, quizId, request);
	}

	@PutMapping("/{quizId}/questions/{questionId}")
	public QuestionResponse updateQuestion(@PathVariable UUID quizId,
			@PathVariable UUID questionId, @Valid @RequestBody QuestionRequest request) {
		LoginUser loginUser = LoginUser.current();
		return quizService.updateQuestion(loginUser, quizId, questionId, request);
	}

	@DeleteMapping("/{quizId}/questions/{questionId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteQuestion(@PathVariable UUID quizId,
			@PathVariable UUID questionId) {
		LoginUser loginUser = LoginUser.current();
		quizService.deleteQuestion(loginUser, quizId, questionId);
	}

	/** Sắp xếp lại câu hỏi: danh sách id đầy đủ theo thứ tự mong muốn. */
	@PutMapping("/{quizId}/questions/order")
	public List<QuestionResponse> reorderQuestions(@PathVariable UUID quizId,
			@Valid @RequestBody ReorderRequest request) {
		LoginUser loginUser = LoginUser.current();
		return quizService.reorderQuestions(loginUser, quizId, request.orderedIds());
	}
}
