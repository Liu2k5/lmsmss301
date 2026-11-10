package com.example.course_service.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.course_service.dto.CourseDetailResponse;
import com.example.course_service.dto.LessonRequest;
import com.example.course_service.dto.MaterialRequest;
import com.example.course_service.dto.ReorderRequest;
import com.example.course_service.dto.SectionRequest;
import com.example.course_service.entity.MaterialType;
import com.example.course_service.security.LoginUser;
import com.example.course_service.service.CourseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Soạn nội dung khoá học: chương, bài học, học liệu.
 * Mọi method chỉ cho INSTRUCTOR và ADMIN (chặn trong SecurityConfig); service kiểm tra thêm có phải giảng viên phụ trách không.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CurriculumController {

	private final CourseService courseService;

	// ================================================================ chương

	@PutMapping("/sections/{sectionId}")
	public CourseDetailResponse updateSection(@CurrentSecurityContext(expression = "authentication") JwtAuthenticationToken auth, @PathVariable UUID sectionId,
			@Valid @RequestBody SectionRequest request) {
		return courseService.updateSection(LoginUser.from(auth), sectionId, request);
	}

	@DeleteMapping("/sections/{sectionId}")
	public CourseDetailResponse deleteSection(@CurrentSecurityContext(expression = "authentication") JwtAuthenticationToken auth, @PathVariable UUID sectionId) {
		return courseService.deleteSection(LoginUser.from(auth), sectionId);
	}

	@PostMapping("/sections/{sectionId}/lessons")
	@ResponseStatus(HttpStatus.CREATED)
	public CourseDetailResponse addLesson(@CurrentSecurityContext(expression = "authentication") JwtAuthenticationToken auth, @PathVariable UUID sectionId,
			@Valid @RequestBody LessonRequest request) {
		return courseService.addLesson(LoginUser.from(auth), sectionId, request);
	}

	@PutMapping("/sections/{sectionId}/lessons/order")
	public CourseDetailResponse reorderLessons(@CurrentSecurityContext(expression = "authentication") JwtAuthenticationToken auth, @PathVariable UUID sectionId,
			@Valid @RequestBody ReorderRequest request) {
		return courseService.reorderLessons(LoginUser.from(auth), sectionId, request.orderedIds());
	}


	@PutMapping("/lessons/{lessonId}")
	public CourseDetailResponse updateLesson(@CurrentSecurityContext(expression = "authentication") JwtAuthenticationToken auth, @PathVariable UUID lessonId,
			@Valid @RequestBody LessonRequest request) {
		return courseService.updateLesson(LoginUser.from(auth), lessonId, request);
	}

	@DeleteMapping("/lessons/{lessonId}")
	public CourseDetailResponse deleteLesson(@CurrentSecurityContext(expression = "authentication") JwtAuthenticationToken auth, @PathVariable UUID lessonId) {
		return courseService.deleteLesson(LoginUser.from(auth), lessonId);
	}

	@PostMapping("/lessons/{lessonId}/materials")
	@ResponseStatus(HttpStatus.CREATED)
	public CourseDetailResponse addMaterial(@CurrentSecurityContext(expression = "authentication") JwtAuthenticationToken auth, @PathVariable UUID lessonId,
			@Valid @RequestBody MaterialRequest request) {
		return courseService.addMaterial(LoginUser.from(auth), lessonId, request);
	}

	/** Upload video / tài liệu lên MinIO (multipart/form-data: file, name?, type?). */
	@PostMapping(value = "/lessons/{lessonId}/materials/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public CourseDetailResponse uploadMaterial(@CurrentSecurityContext(expression = "authentication") JwtAuthenticationToken auth, @PathVariable UUID lessonId,
			@RequestPart("file") MultipartFile file,
			@RequestParam(required = false) String name,
			@RequestParam(required = false) MaterialType type) {
		return courseService.uploadMaterial(LoginUser.from(auth), lessonId, file, name, type);
	}

	// ================================================================ học liệu

	@PutMapping("/materials/{materialId}")
	public CourseDetailResponse updateMaterial(@CurrentSecurityContext(expression = "authentication") JwtAuthenticationToken auth, @PathVariable UUID materialId,
			@Valid @RequestBody MaterialRequest request) {
		return courseService.updateMaterial(LoginUser.from(auth), materialId, request);
	}

	@DeleteMapping("/materials/{materialId}")
	public CourseDetailResponse deleteMaterial(@CurrentSecurityContext(expression = "authentication") JwtAuthenticationToken auth, @PathVariable UUID materialId) {
		return courseService.deleteMaterial(LoginUser.from(auth), materialId);
	}
}
