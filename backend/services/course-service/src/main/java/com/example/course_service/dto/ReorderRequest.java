package com.example.course_service.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** Thứ tự mới: danh sách id đầy đủ theo đúng thứ tự mong muốn (UC-C10). */
public record ReorderRequest(
		@NotEmpty(message = "Danh sách thứ tự không được để trống")
		List<@NotNull(message = "Id không được để trống") Integer> orderedIds) {
}
