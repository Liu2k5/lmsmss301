package com.example.course_service.repository;

/** Projection đếm số bản ghi theo khoá học (dùng cho danh sách khoá học). */
public interface CourseCount {

	Integer getCourseId();

	Long getTotal();
}
