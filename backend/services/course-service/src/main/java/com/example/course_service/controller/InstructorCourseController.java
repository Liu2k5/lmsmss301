package com.example.course_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.course_service.dto.CourseDetailResponse;
import com.example.course_service.dto.CourseSummaryResponse;
import com.example.course_service.entity.CourseStatus;
import com.example.course_service.security.LoginUser;
import com.example.course_service.service.CourseService;

import lombok.RequiredArgsConstructor;

/**
 * "Khoá học của tôi" cho giảng viên: chỉ khoá đứng tên chính mình, gồm cả bản nháp.
 * Chỉ INSTRUCTOR gọi được (chặn trong SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/instructor/courses")
@RequiredArgsConstructor
public class InstructorCourseController {

	private final CourseService courseService;

	@GetMapping
	public List<CourseSummaryResponse> myCourses(
			@CurrentSecurityContext(expression = "authentication") JwtAuthenticationToken auth,
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) CourseStatus status,
			@RequestParam(required = false) UUID categoryId) {
		return courseService.searchMine(LoginUser.from(auth), keyword, status, categoryId);
	}

	@GetMapping("/{id}")
	public CourseDetailResponse myCourse(
			@CurrentSecurityContext(expression = "authentication") JwtAuthenticationToken auth,
			@PathVariable UUID id) {
		return courseService.getMyDetail(LoginUser.from(auth), id);
	}
}
