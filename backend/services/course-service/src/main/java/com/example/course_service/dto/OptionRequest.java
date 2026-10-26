package com.example.course_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Một đáp án trong câu hỏi. {@code position} được server tự sinh theo thứ tự trong mảng,
 * client không cần gửi lên.
 */
public record OptionRequest(

		/** Chỉ dùng khi cập nhật; null nghĩa là tạo mới. */
		Integer id,

		@NotBlank(message = "Nội dung đáp án không được để trống")
		@Size(max = 2000, message = "Nội dung đáp án tối đa 2000 ký tự")
		String content,

		boolean correct) {
}
