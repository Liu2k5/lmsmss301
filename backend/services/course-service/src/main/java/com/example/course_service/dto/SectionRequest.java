package com.example.course_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SectionRequest(

		@NotBlank(message = "Tên chương không được để trống")
		@Size(max = 255, message = "Tên chương tối đa 255 ký tự")
		String title,

		@Size(max = 5000, message = "Mô tả tối đa 5000 ký tự")
		String description) {
}
