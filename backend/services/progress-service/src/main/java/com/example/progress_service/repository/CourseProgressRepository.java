package com.example.progress_service.repository;

import com.example.progress_service.entity.CourseProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface CourseProgressRepository
        extends JpaRepository<CourseProgress, UUID> {

    Optional<CourseProgress> findByStudentIdAndCourseId(
            UUID studentId,
            UUID courseId
    );

    List<CourseProgress> findByStudentId(UUID studentId);

    List<CourseProgress> findByCourseId(UUID courseId);

    List<CourseProgress> findByCourseIdAndStatus(
            UUID courseId,
            String status
    );

    boolean existsByStudentIdAndCourseId(
            UUID studentId,
            UUID courseId
    );

    long countByCourseId(UUID courseId);

    long countByCourseIdAndStatus(
            UUID courseId,
            String status
    );
}