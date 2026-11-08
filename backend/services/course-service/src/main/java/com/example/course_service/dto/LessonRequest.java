package com.example.course_service.dto;

import com.example.course_service.entity.LessonType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LessonRequest(

		@NotBlank(message = "Tên bài học không được để trống")
		@Size(max = 255, message = "Tên bài học tối đa 255 ký tự")
		String title,

		@Size(max = 5000, message = "Mô tả tối đa 5000 ký tự")
		String description,

		@NotNull(message = "Loại bài học không được để trống")
		LessonType type,

		/** Bài học xem thử miễn phí (UC-C20). Bỏ trống = false. */
		Boolean preview) {
}
