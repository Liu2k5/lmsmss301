package com.example.course_service.repository;

import java.util.UUID;
/** Projection đếm số bản ghi theo khoá học (dùng cho danh sách khoá học). */
public interface CourseCount {

	UUID getCourseId();

	Long getTotal();
}
