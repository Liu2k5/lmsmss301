package com.example.course_service.dto;

import com.example.course_service.entity.MaterialType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Thêm / sửa học liệu dạng liên kết, hoặc sửa thông tin học liệu đã upload lên MinIO.
 * File thật được upload qua {@code POST /lessons/{id}/materials/upload}.
 */
public record MaterialRequest(

		@NotBlank(message = "Tên học liệu không được để trống")
		@Size(max = 255, message = "Tên học liệu tối đa 255 ký tự")
		String name,

		@NotNull(message = "Loại học liệu không được để trống")
		MaterialType type,

		/** Bắt buộc với học liệu dạng link. Để trống khi sửa học liệu đã upload = giữ nguyên file. */
		@Size(max = 1000, message = "URL tối đa 1000 ký tự")
		@Pattern(regexp = "^$|^https?://.+", message = "URL phải bắt đầu bằng http:// hoặc https://")
		String url,

		@PositiveOrZero(message = "Dung lượng không được âm")
		Long fileSize,

		@Size(max = 100, message = "MIME type tối đa 100 ký tự")
		String mimeType) {
}
