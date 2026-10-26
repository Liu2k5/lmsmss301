package com.example.course_service.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record QuizRequest(

		@NotNull(message = "Bài học không được để trống")
		Integer lessonId,

		@NotBlank(message = "Tên quiz không được để trống")
		@Size(max = 255, message = "Tên quiz tối đa 255 ký tự")
		String title,

		@Size(max = 5000, message = "Mô tả tối đa 5000 ký tự")
		String description,

		@NotNull(message = "Điểm đạt không được để trống")
		@DecimalMin(value = "0.00", message = "Điểm đạt tối thiểu là 0")
		@DecimalMax(value = "100.00", message = "Điểm đạt tối đa là 100")
		@Digits(integer = 3, fraction = 2, message = "Điểm đạt tối đa 2 chữ số thập phân")
		BigDecimal passingScore,

		@NotEmpty(message = "Quiz phải có ít nhất 1 câu hỏi")
		@Valid
		List<QuestionRequest> questions) {
}
