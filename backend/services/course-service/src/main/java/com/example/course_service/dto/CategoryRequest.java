package com.example.course_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Dữ liệu tạo / sửa category (chỉ Admin). */
public record CategoryRequest(

		@NotBlank(message = "Tên danh mục không được để trống")
		@Size(max = 100, message = "Tên danh mục tối đa 100 ký tự")
		String name,

		@Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
		String description) {
}
