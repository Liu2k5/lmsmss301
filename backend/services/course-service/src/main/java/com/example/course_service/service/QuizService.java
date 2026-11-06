package com.example.course_service.service;

import java.util.List;

import com.example.course_service.dto.LessonOptionResponse;
import com.example.course_service.dto.QuizRequest;
import com.example.course_service.dto.QuizResponse;
import com.example.course_service.dto.QuizSummaryResponse;

/** Quản lý quiz của bài học loại QUIZ (UC-C14 → UC-C19). */
public interface QuizService {

	/** Danh sách quiz, lọc theo bài học nếu {@code lessonId} khác null. */
	List<QuizSummaryResponse> findAll(Integer lessonId);

	QuizResponse findById(Integer id);

	/** Bài học loại QUIZ kèm quiz hiện có (nếu đã có) để client chọn khi tạo mới. */
	List<LessonOptionResponse> findLessonOptions();

	QuizResponse create(QuizRequest request);

	QuizResponse update(Integer id, QuizRequest request);

	void delete(Integer id);
}
