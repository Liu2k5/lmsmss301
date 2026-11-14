package com.example.course_service.controller;

import java.util.List;
import java.util.UUID;

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


@RestController
@RequestMapping("/api/v1/instructor/courses")
@RequiredArgsConstructor
public class InstructorCourseController {

	private final CourseService courseService;

	@GetMapping
	public List<CourseSummaryResponse> myCourses(
			
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) CourseStatus status,
			@RequestParam(required = false) UUID categoryId) {
		LoginUser loginUser = LoginUser.current();
		return courseService.searchMine(loginUser, keyword, status, categoryId);
	}

	@GetMapping("/{id}")
	public CourseDetailResponse myCourse(
			
			@PathVariable UUID id) {
		LoginUser loginUser = LoginUser.current();
		return courseService.getMyDetail(loginUser, id);
	}
}
