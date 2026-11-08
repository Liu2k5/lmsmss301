package com.example.review_rating_service.repository;

import com.example.review_rating_service.entity.CourseRatingSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseRatingSummaryRepository extends JpaRepository<CourseRatingSummary, UUID> {
    Optional<CourseRatingSummary> findByCourseId(UUID courseId);
}
