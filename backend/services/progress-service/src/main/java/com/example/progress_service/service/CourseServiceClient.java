package com.example.progress_service.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.progress_service.dto.QuizAnswerKey;

/** Gọi API nội bộ của Course Service. */
public interface CourseServiceClient {

    List<UUID> getRequiredQuizIds(UUID courseId);

    /** Đề + đáp án đúng của quiz; rỗng nếu quiz không tồn tại hoặc Course Service không sẵn sàng. */
    Optional<QuizAnswerKey> fetchAnswerKey(UUID quizId);

    /** Danh sách bài học đã xuất bản của khoá học (dùng để khởi tạo tiến độ khi event bị lỡ). */
    List<UUID> fetchPublishedLessonIds(UUID courseId);
}
