package com.example.course_service.entity;

/**
 * Trạng thái vòng đời của khoá học: DRAFT -> PENDING_REVIEW -> PUBLISHED,
 * có thể bị REJECTED khi duyệt, UNPUBLISHED khi ẩn, hoặc ARCHIVED khi lưu trữ.
 */
public enum CourseStatus {
	DRAFT,
	PENDING_REVIEW,
	PUBLISHED,
	REJECTED,
	UNPUBLISHED,
	ARCHIVED
}
