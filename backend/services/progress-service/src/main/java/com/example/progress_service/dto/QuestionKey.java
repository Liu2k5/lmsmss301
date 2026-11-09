package com.example.progress_service.dto;

import java.util.List;
import java.util.UUID;

/** Đáp án đúng của một câu hỏi (nhận từ Course Service, chỉ dùng để chấm điểm). */
public record QuestionKey(UUID questionId, String type, List<UUID> correctOptionIds) {
}
