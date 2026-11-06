package com.example.course_service.entity;

/**
 * Trạng thái vòng đời của khoá học: DRAFT -> PUBLISHED, có thể UNPUBLISHED khi ẩn
 * hoặc ARCHIVED khi lưu trữ. Giảng viên tự xuất bản, không có bước duyệt.
 */
public enum CourseStatus {
	DRAFT,
	PUBLISHED,
	UNPUBLISHED,
	ARCHIVED
}
