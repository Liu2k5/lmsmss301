package com.example.course_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
import com.example.course_service.dto.CourseRequest;
import com.example.course_service.dto.CourseStatusRequest;
import com.example.course_service.dto.CourseSummaryResponse;
import com.example.course_service.dto.PublishedCurriculumResponse;
import com.example.course_service.dto.ReorderRequest;
import com.example.course_service.dto.SectionRequest;
import com.example.course_service.entity.CourseStatus;
import com.example.course_service.security.LoginUser;
import com.example.course_service.service.CourseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Khoá học.
 * <ul>
 *   <li>Xem (GET): công khai, chỉ khoá không phải bản nháp. Giảng viên xem khoá của mình
 *       (cả bản nháp) qua {@link InstructorCourseController}.</li>
 *   <li>Tạo / sửa / xoá: chỉ INSTRUCTOR (chặn trong SecurityConfig);
 *       service kiểm tra thêm có phải giảng viên phụ trách khoá học không.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

	private final CourseService courseService;

	@GetMapping
	public List<CourseSummaryResponse> search(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) CourseStatus status,
			@RequestParam(required = false) UUID instructorId,
			@RequestParam(required = false) UUID categoryId) {
		return courseService.search(keyword, status, instructorId, categoryId);
	}

	@GetMapping("/{id}")
	public CourseDetailResponse findById(@PathVariable UUID id) {
		return courseService.getDetail(id);
	}

	@GetMapping("/{id}/curriculum/published")
	public PublishedCurriculumResponse publishedCurriculum(@PathVariable UUID id) {
		return courseService.getPublishedCurriculum(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CourseDetailResponse create(@CurrentSecurityContext(expression = "authentication") JwtAuthenticationToken auth,
			@Valid @RequestBody CourseRequest request) {
		return courseService.create(LoginUser.from(auth), request);
	}

	@PutMapping("/{id}")
	public CourseDetailResponse update(@CurrentSecurityContext(expression = "authentication") JwtAuthenticationToken auth, @PathVariable UUID id,
			@Valid @RequestBody CourseRequest request) {
		return courseService.update(LoginUser.from(auth), id, request);
	}

	/** Upload ảnh bìa từ máy lên MinIO (multipart/form-data: file). */
	@PostMapping(value = "/{id}/thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public CourseDetailResponse uploadThumbnail(@CurrentSecurityContext(expression = "authentication") JwtAuthenticationToken auth, @PathVariable UUID id,
			@RequestPart("file") MultipartFile file) {
		return courseService.uploadThumbnail(LoginUser.from(auth), id, file);
	}

	@DeleteMapping("/{id}/thumbnail")
	public CourseDetailResponse removeThumbnail(@CurrentSecurityContext(expression = "authentication") JwtAuthenticationToken auth, @PathVariable UUID id) {
		return courseService.removeThumbnail(LoginUser.from(auth), id);
	}

	@PatchMapping("/{id}/status")
	public CourseDetailResponse changeStatus(@CurrentSecurityContext(expression = "authentication") JwtAuthenticationToken auth, @PathVariable UUID id,
			@Valid @RequestBody CourseStatusRequest request) {
		return courseService.changeStatus(LoginUser.from(auth), id, request.status());
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@CurrentSecurityContext(expression = "authentication") JwtAuthenticationToken auth, @PathVariable UUID id) {
		courseService.delete(LoginUser.from(auth), id);
	}

	@PostMapping("/{id}/sections")
	@ResponseStatus(HttpStatus.CREATED)
	public CourseDetailResponse addSection(@CurrentSecurityContext(expression = "authentication") JwtAuthenticationToken auth, @PathVariable UUID id,
			@Valid @RequestBody SectionRequest request) {
		return courseService.addSection(LoginUser.from(auth), id, request);
	}

	@PutMapping("/{id}/sections/order")
	public CourseDetailResponse reorderSections(@CurrentSecurityContext(expression = "authentication") JwtAuthenticationToken auth, @PathVariable UUID id,
			@Valid @RequestBody ReorderRequest request) {
		return courseService.reorderSections(LoginUser.from(auth), id, request.orderedIds());
	}
}
