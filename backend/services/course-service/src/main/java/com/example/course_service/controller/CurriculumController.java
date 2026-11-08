package com.example.course_service.controller;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.course_service.dto.CourseDetailResponse;
import com.example.course_service.dto.LessonRequest;
import com.example.course_service.dto.MaterialRequest;
import com.example.course_service.dto.ReorderRequest;
import com.example.course_service.dto.SectionRequest;
import com.example.course_service.entity.MaterialType;
import com.example.course_service.service.CourseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CurriculumController {

	private final CourseService courseService;


	@PutMapping("/sections/{sectionId}")
	public CourseDetailResponse updateSection(@PathVariable UUID sectionId,
			@Valid @RequestBody SectionRequest request) {
		return courseService.updateSection(sectionId, request);
	}

	@DeleteMapping("/sections/{sectionId}")
	public CourseDetailResponse deleteSection(@PathVariable UUID sectionId) {
		return courseService.deleteSection(sectionId);
	}

	@PostMapping("/sections/{sectionId}/lessons")
	@ResponseStatus(HttpStatus.CREATED)
	public CourseDetailResponse addLesson(@PathVariable UUID sectionId,
			@Valid @RequestBody LessonRequest request) {
		return courseService.addLesson(sectionId, request);
	}

	@PutMapping("/sections/{sectionId}/lessons/order")
	public CourseDetailResponse reorderLessons(@PathVariable UUID sectionId,
			@Valid @RequestBody ReorderRequest request) {
		return courseService.reorderLessons(sectionId, request.orderedIds());
	}


	@PutMapping("/lessons/{lessonId}")
	public CourseDetailResponse updateLesson(@PathVariable UUID lessonId,
			@Valid @RequestBody LessonRequest request) {
		return courseService.updateLesson(lessonId, request);
	}

	@DeleteMapping("/lessons/{lessonId}")
	public CourseDetailResponse deleteLesson(@PathVariable UUID lessonId) {
		return courseService.deleteLesson(lessonId);
	}

	@PostMapping("/lessons/{lessonId}/materials")
	@ResponseStatus(HttpStatus.CREATED)
	public CourseDetailResponse addMaterial(@PathVariable UUID lessonId,
			@Valid @RequestBody MaterialRequest request) {
		return courseService.addMaterial(lessonId, request);
	}

	/** Upload video / tài liệu lên MinIO (multipart/form-data: file, name?, type?). */
	@PostMapping(value = "/lessons/{lessonId}/materials/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public CourseDetailResponse uploadMaterial(@PathVariable UUID lessonId,
			@RequestPart("file") MultipartFile file,
			@RequestParam(required = false) String name,
			@RequestParam(required = false) MaterialType type) {
		return courseService.uploadMaterial(lessonId, file, name, type);
	}


	@PutMapping("/materials/{materialId}")
	public CourseDetailResponse updateMaterial(@PathVariable UUID materialId,
			@Valid @RequestBody MaterialRequest request) {
		return courseService.updateMaterial(materialId, request);
	}

	@DeleteMapping("/materials/{materialId}")
	public CourseDetailResponse deleteMaterial(@PathVariable UUID materialId) {
		return courseService.deleteMaterial(materialId);
	}
}
