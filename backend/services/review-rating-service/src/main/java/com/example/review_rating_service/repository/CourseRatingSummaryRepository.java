package com.example.review_rating_service.repository;

import com.example.review_rating_service.entity.CourseRatingSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseRatingSummaryRepository extends JpaRepository<CourseRatingSummary, Integer> {
    Optional<CourseRatingSummary> findByCourseId(Integer courseId);
}
