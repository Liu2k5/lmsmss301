package com.example.course_service.dto;

import java.util.List;

import com.example.course_service.entity.QuestionType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Một câu hỏi trong quiz. {@code position} được server tự sinh theo thứ tự trong mảng.
 */
public record QuestionRequest(

		/** Chỉ dùng khi cập nhật; null nghĩa là tạo mới. */
		Integer id,

		@NotBlank(message = "Nội dung câu hỏi không được để trống")
		String content,

		@NotNull(message = "Loại câu hỏi không được để trống")
		QuestionType type,

		@NotEmpty(message = "Câu hỏi phải có ít nhất 1 đáp án")
		@Valid
		List<OptionRequest> options) {
}
