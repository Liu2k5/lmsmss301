package com.example.enrollment_service.service;

import com.example.enrollment_service.domain.AccessStatus;
import com.example.enrollment_service.domain.AccessType;
import com.example.enrollment_service.domain.EnrollEvent;
import com.example.enrollment_service.domain.Enrollment;
import com.example.enrollment_service.domain.EnrollmentAccess;
import com.example.enrollment_service.domain.EventType;
import com.example.enrollment_service.domain.Status;
import com.example.enrollment_service.repository.EnrollEventRepository;
import com.example.enrollment_service.repository.EnrollmentAccessRepository;
import com.example.enrollment_service.repository.EnrollmentRepository;
import com.example.enrollment_service.web.dto.CheckResponse;
import com.example.enrollment_service.web.dto.EnrollmentResponse;
import com.example.enrollment_service.web.exception.ConflictException;
import com.example.enrollment_service.web.exception.NotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class EnrollmentService {

	public static final String CHECK_CACHE = "enrollment-check";

	private final EnrollmentRepository enrollments;
	private final EnrollmentAccessRepository accesses;
	private final EnrollEventRepository events;

	public EnrollmentService(EnrollmentRepository enrollments,
							 EnrollmentAccessRepository accesses,
							 EnrollEventRepository events) {
		this.enrollments = enrollments;
		this.accesses = accesses;
		this.events = events;
	}

	@Transactional
	@CacheEvict(cacheNames = CHECK_CACHE, key = "#studentId + ':' + #courseId")
	public EnrollmentResponse enroll(UUID studentId, UUID courseId) {
		var existing = enrollments.findByStudentIdAndCourseIdAndDeletedAtIsNull(studentId, courseId);
		if (existing.isPresent() && existing.get().getStatus() != Status.CANCELLED) {
			throw new ConflictException("Cannot enroll while status is " + existing.get().getStatus());
		}
		return activate(studentId, courseId, EventType.ENROLLED);
	}

	@Transactional
	@CacheEvict(cacheNames = CHECK_CACHE, key = "#studentId + ':' + #courseId")
	public void cancel(UUID studentId, UUID courseId) {
		Instant now = Instant.now();
		Enrollment enrollment = findOrThrow(studentId, courseId);
		// Do not let a student turn an admin suspension into a re-enrollable cancellation.
		if (enrollment.getStatus() != Status.ACTIVE && enrollment.getStatus() != Status.CANCELLED) {
			throw new ConflictException("Cannot cancel while status is " + enrollment.getStatus());
		}
		enrollment.setStatus(Status.CANCELLED);
		enrollment.setCancelledAt(now);
		enrollment.setAccessGranted(false);
		enrollment.setUpdatedAt(now);
		enrollments.save(enrollment);
		recordAccess(enrollment, AccessStatus.REVOKED, now, "CANCELLED");
		recordEvent(enrollment, EventType.CANCELLED, now);
	}

	@Transactional(readOnly = true)
	public List<EnrollmentResponse> myEnrollments(UUID studentId) {
		return enrollments.findAllByStudentIdAndDeletedAtIsNull(studentId).stream()
				.map(EnrollmentResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<EnrollmentResponse> activeStudentsOfCourse(UUID courseId) {
		return enrollments.findAllByCourseIdAndStatus(courseId, Status.ACTIVE).stream()
				.map(EnrollmentResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	@Cacheable(cacheNames = CHECK_CACHE, key = "#studentId + ':' + #courseId")
	public CheckResponse check(UUID studentId, UUID courseId) {
		return enrollments.findByStudentIdAndCourseIdAndDeletedAtIsNull(studentId, courseId)
				.map(e -> CheckResponse.of(e, Instant.now()))
				.orElseGet(CheckResponse::none);
	}

	@Transactional
	@CacheEvict(cacheNames = CHECK_CACHE, key = "#studentId + ':' + #courseId")
	public EnrollmentResponse grant(UUID studentId, UUID courseId) {
		return activate(studentId, courseId, EventType.GRANTED);
	}

	@Transactional
	@CacheEvict(cacheNames = CHECK_CACHE, key = "#studentId + ':' + #courseId")
	public void revoke(UUID studentId, UUID courseId) {
		Instant now = Instant.now();
		Enrollment enrollment = findOrThrow(studentId, courseId);
		enrollment.setStatus(Status.SUSPENDED);
		enrollment.setAccessGranted(false);
		enrollment.setUpdatedAt(now);
		enrollments.save(enrollment);
		recordAccess(enrollment, AccessStatus.REVOKED, now, "REVOKED");
		recordEvent(enrollment, EventType.REVOKED, now);
	}

	private EnrollmentResponse activate(UUID studentId, UUID courseId, EventType eventType) {
		Instant now = Instant.now();
		Enrollment enrollment = enrollments.findByStudentIdAndCourseIdAndDeletedAtIsNull(studentId, courseId)
				.orElseGet(() -> newEnrollment(studentId, courseId, now));
		enrollment.setStatus(Status.ACTIVE);
		enrollment.setCancelledAt(null);
		enrollment.setAccessGranted(true);
		enrollment.setUpdatedAt(now);
		enrollments.save(enrollment);
		recordAccess(enrollment, AccessStatus.GRANTED, now, eventType.name());
		recordEvent(enrollment, eventType, now);
		return EnrollmentResponse.from(enrollment);
	}

	private Enrollment newEnrollment(UUID studentId, UUID courseId, Instant now) {
		Enrollment enrollment = new Enrollment();
		enrollment.setId(UUID.randomUUID());
		enrollment.setStudentId(studentId);
		enrollment.setCourseId(courseId);
		enrollment.setEnrolledAt(now);
		enrollment.setCreatedAt(now);
		return enrollment;
	}

	private Enrollment findOrThrow(UUID studentId, UUID courseId) {
		return enrollments.findByStudentIdAndCourseIdAndDeletedAtIsNull(studentId, courseId)
				.orElseThrow(() -> new NotFoundException("Enrollment not found"));
	}

	private void recordAccess(Enrollment enrollment, AccessStatus status, Instant now, String reason) {
		EnrollmentAccess access = new EnrollmentAccess();
		access.setId(UUID.randomUUID());
		access.setEnrollmentId(enrollment.getId());
		access.setAccessType(AccessType.STANDARD);
		access.setStatus(status);
		access.setGrantedAt(now);
		if (status == AccessStatus.REVOKED) {
			access.setRevokedAt(now);
		}
		access.setReason(reason);
		accesses.save(access);
	}

	private void recordEvent(Enrollment enrollment, EventType type, Instant now) {
		EnrollEvent event = new EnrollEvent();
		event.setId(UUID.randomUUID());
		event.setEnrollmentId(enrollment.getId());
		event.setEventType(type);
		event.setCreatedAt(now);
		events.save(event);
	}
}
