package com.example.enrollment_service.repository;

import com.example.enrollment_service.domain.Enrollment;
import com.example.enrollment_service.domain.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

	Optional<Enrollment> findByStudentIdAndCourseIdAndDeletedAtIsNull(UUID studentId, UUID courseId);

	List<Enrollment> findAllByStudentIdAndDeletedAtIsNull(UUID studentId);

	List<Enrollment> findAllByCourseIdAndStatus(UUID courseId, Status status);
}
