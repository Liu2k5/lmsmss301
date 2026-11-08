package com.example.course_service.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.example.course_service.entity.CourseStatus;

/** Chi tiết khoá học kèm toàn bộ curriculum (chương → bài học → học liệu). */
public record CourseDetailResponse(
		UUID id,
		UUID instructorId,
		String title,
		String description,
		String thumbnailUrl,
		CourseStatus status,
		/** Các trạng thái có thể chuyển tới từ trạng thái hiện tại. */
		List<CourseStatus> allowedTransitions,
		boolean editable,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		List<SectionResponse> sections) {
}
