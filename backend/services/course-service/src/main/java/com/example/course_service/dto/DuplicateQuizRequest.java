package com.example.course_service.dto;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;

/** Nhân bản quiz sang một bài học loại QUIZ khác chưa có quiz. */
public record DuplicateQuizRequest(
		@NotNull(message = "Bài học đích không được để trống")
		UUID lessonId) {
}
