package com.example.enrollment_service.web.dto;

import com.example.enrollment_service.domain.Enrollment;

import java.time.Instant;
import java.util.UUID;

public record EnrollmentResponse(UUID id, UUID studentId, UUID courseId, String status,
								 Instant enrolledAt, Instant cancelledAt, Instant expiresAt,
								 boolean accessGranted) {

	public static EnrollmentResponse from(Enrollment e) {
		return new EnrollmentResponse(e.getId(), e.getStudentId(), e.getCourseId(), e.getStatus().name(),
				e.getEnrolledAt(), e.getCancelledAt(), e.getExpiresAt(), e.isAccessGranted());
	}
}
