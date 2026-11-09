package com.example.course_service.security;

import org.springframework.stereotype.Component;

import com.example.course_service.entity.Course;

import lombok.RequiredArgsConstructor;

/** Quiz chỉ do giảng viên phụ trách khoá học được sửa (Admin được phép). */
@Component
@RequiredArgsConstructor
public class QuizAccessGuard {

	private final CourseAccessGuard courseAccessGuard;

	public void checkCanManage(Course course) {
		courseAccessGuard.checkCanManage(course,
				"Chỉ giảng viên phụ trách khoá học mới được sửa quiz của khoá học này");
	}
}
