package com.example.course_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
		// Endpoint công khai: chưa đăng nhập thì LoginUser.current() trả về khách (id = null)
		LoginUser loginUser = LoginUser.current();
		return courseService.getDetail(loginUser, id);
	}

	@GetMapping("/{id}/curriculum/published")
	public PublishedCurriculumResponse publishedCurriculum(@PathVariable UUID id) {
		return courseService.getPublishedCurriculum(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CourseDetailResponse create(
			@Valid @RequestBody CourseRequest request) {
		LoginUser loginUser = LoginUser.current();
		return courseService.create(loginUser, request);
	}

	@PutMapping("/{id}")
	public CourseDetailResponse update(@PathVariable UUID id,
			@Valid @RequestBody CourseRequest request) {
		LoginUser loginUser = LoginUser.current();
		return courseService.update(loginUser, id, request);
	}

	@PostMapping(value = "/{id}/thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public CourseDetailResponse uploadThumbnail(@PathVariable UUID id,
			@RequestPart("file") MultipartFile file) {
		LoginUser loginUser = LoginUser.current();
		return courseService.uploadThumbnail(loginUser, id, file);
	}

	@DeleteMapping("/{id}/thumbnail")
	public CourseDetailResponse removeThumbnail(@PathVariable UUID id) {
		LoginUser loginUser = LoginUser.current();
		return courseService.removeThumbnail(loginUser, id);
	}

	@PatchMapping("/{id}/status")
	public CourseDetailResponse changeStatus(@PathVariable UUID id,
			@Valid @RequestBody CourseStatusRequest request) {
		LoginUser loginUser = LoginUser.current();
		return courseService.changeStatus(loginUser, id, request.status());
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable UUID id) {
		LoginUser loginUser = LoginUser.current();
		courseService.delete(loginUser, id);
	}

	@PostMapping("/{id}/sections")
	@ResponseStatus(HttpStatus.CREATED)
	public CourseDetailResponse addSection(@PathVariable UUID id,
			@Valid @RequestBody SectionRequest request) {
		LoginUser loginUser = LoginUser.current();
		return courseService.addSection(loginUser, id, request);
	}

	@PutMapping("/{id}/sections/order")
	public CourseDetailResponse reorderSections(@PathVariable UUID id,
			@Valid @RequestBody ReorderRequest request) {
		LoginUser loginUser = LoginUser.current();
		return courseService.reorderSections(loginUser, id, request.orderedIds());
	}
}
