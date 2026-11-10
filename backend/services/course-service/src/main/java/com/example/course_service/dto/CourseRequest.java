package com.example.course_service.dto;

import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Dữ liệu tạo / sửa thông tin cơ bản của khoá học (UC-C01, UC-C02). */
public record CourseRequest(

		/** Tạm truyền từ client; khi có Gateway sẽ lấy từ header X-User-Id. */
		@NotNull(message = "Giảng viên không được để trống")
		UUID instructorId,

		@NotBlank(message = "Tên khoá học không được để trống")
		@Size(max = 255, message = "Tên khoá học tối đa 255 ký tự")
		String title,

		@Size(max = 10000, message = "Mô tả tối đa 10000 ký tự")
		String description,

		@Size(max = 500, message = "URL ảnh bìa tối đa 500 ký tự")
		@Pattern(regexp = "^$|^https?://.+", message = "URL ảnh bìa phải bắt đầu bằng http:// hoặc https://")
		String thumbnailUrl,

		/** Danh mục của khoá học; để trống (null) = khoá học chưa có danh mục. */
		UUID categoryId) {
}
