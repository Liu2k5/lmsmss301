package com.example.review_rating_service.repository;

import com.example.review_rating_service.entity.ReviewPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewPermissionRepository extends JpaRepository<ReviewPermission, Long> {

    Optional<ReviewPermission> findByCourseIdAndStudentId(UUID courseId, UUID studentId);

    boolean existsByCourseIdAndStudentId(UUID courseId, UUID studentId);
}
