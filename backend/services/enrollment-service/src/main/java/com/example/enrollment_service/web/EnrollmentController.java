package com.example.enrollment_service.web;

import com.example.enrollment_service.service.EnrollmentService;
import com.example.enrollment_service.web.dto.CheckResponse;
import com.example.enrollment_service.web.dto.EnrollRequest;
import com.example.enrollment_service.web.dto.EnrollmentResponse;
import com.example.enrollment_service.web.dto.TargetRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/enrollments")
public class EnrollmentController {

	private final EnrollmentService service;

	public EnrollmentController(EnrollmentService service) {
		this.service = service;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public EnrollmentResponse enroll(@RequestHeader("X-User-Id") UUID userId,
									 @Valid @RequestBody EnrollRequest request) {
		return service.enroll(userId, request.courseId());
	}

	@DeleteMapping("/{courseId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void cancel(@RequestHeader("X-User-Id") UUID userId, @PathVariable UUID courseId) {
		service.cancel(userId, courseId);
	}

	@GetMapping("/me")
	public List<EnrollmentResponse> me(@RequestHeader("X-User-Id") UUID userId) {
		return service.myEnrollments(userId);
	}

	@GetMapping("/check")
	public CheckResponse check(@RequestParam UUID studentId, @RequestParam UUID courseId) {
		return service.check(studentId, courseId);
	}

	@GetMapping("/courses/{courseId}/students")
	public List<EnrollmentResponse> students(@PathVariable UUID courseId) {
		return service.activeStudentsOfCourse(courseId);
	}

	@PostMapping("/admin/grant")
	public EnrollmentResponse grant(@Valid @RequestBody TargetRequest request) {
		return service.grant(request.studentId(), request.courseId());
	}

	@PostMapping("/admin/revoke")
	public void revoke(@Valid @RequestBody TargetRequest request) {
		service.revoke(request.studentId(), request.courseId());
	}
}
