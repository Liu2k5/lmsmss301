package com.example.enrollment_service.repository;

import com.example.enrollment_service.domain.EnrollmentAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EnrollmentAccessRepository extends JpaRepository<EnrollmentAccess, UUID> {

	List<EnrollmentAccess> findAllByEnrollmentId(UUID enrollmentId);
}
