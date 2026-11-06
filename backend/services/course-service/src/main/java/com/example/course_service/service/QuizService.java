package com.example.course_service.service;

import java.util.List;

import com.example.course_service.dto.LessonOptionResponse;
import com.example.course_service.dto.QuestionRequest;
import com.example.course_service.dto.QuestionResponse;
import com.example.course_service.dto.QuizPreviewResponse;
import com.example.course_service.dto.QuizRequest;
import com.example.course_service.dto.QuizResponse;
import com.example.course_service.dto.QuizSummaryResponse;

/**
 * Quản lý quiz của bài học loại QUIZ (UC-C14 → UC-C19).
 *
 * <p>Hai cách thao tác:
 * <ul>
 *   <li><b>Toàn bộ quiz</b> — {@link #create}/{@link #update} nhận cả cây câu hỏi + đáp án (dùng cho form lớn).</li>
 *   <li><b>Từng câu hỏi</b> — thêm/sửa/xoá/sắp xếp riêng lẻ (dùng cho chỉnh sửa nhanh, tránh gửi lại cả quiz).</li>
 * </ul>
 */
public interface QuizService {

	/**
	 * Danh sách quiz kèm số câu hỏi. Các tham số lọc đều tuỳ chọn và kết hợp với nhau (AND).
	 *
	 * @param lessonId chỉ lấy quiz của một bài học
	 * @param courseId chỉ lấy quiz thuộc một khoá học
	 * @param keyword  tìm trong tên quiz, tên bài học, tên khoá học (không phân biệt hoa thường)
	 */
	List<QuizSummaryResponse> findAll(Integer lessonId, Integer courseId, String keyword);

	QuizResponse findById(Integer id);

	/** Bài học loại QUIZ kèm quiz hiện có (nếu đã có) để client chọn khi tạo mới. */
	List<LessonOptionResponse> findLessonOptions();

	QuizResponse create(QuizRequest request);

	QuizResponse update(Integer id, QuizRequest request);

	/** Xoá quiz; câu hỏi và đáp án bị xoá theo (cascade + orphanRemoval). */
	void delete(Integer id);

	/** Nhân bản quiz (kèm toàn bộ câu hỏi/đáp án) sang bài học khác chưa có quiz. */
	QuizResponse duplicate(Integer id, Integer targetLessonId);

	/** Bản xem trước như học viên thấy — không có đáp án đúng. */
	QuizPreviewResponse preview(Integer id);

	// ------------------------------------------------------------ câu hỏi

	QuestionResponse addQuestion(Integer quizId, QuestionRequest request);

	QuestionResponse updateQuestion(Integer quizId, Integer questionId, QuestionRequest request);

	void deleteQuestion(Integer quizId, Integer questionId);

	/** Sắp xếp lại câu hỏi; {@code orderedIds} phải chứa đúng toàn bộ id câu hỏi của quiz. */
	List<QuestionResponse> reorderQuestions(Integer quizId, List<Integer> orderedIds);
}
