package com.example.progress_service.repository;

import com.example.progress_service.entity.CourseProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseProgressRepository
        extends JpaRepository<CourseProgress, Integer> {

    Optional<CourseProgress> findByStudentIdAndCourseId(
            Integer studentId,
            Integer courseId
    );

    List<CourseProgress> findByStudentId(Integer studentId);

    List<CourseProgress> findByCourseId(Integer courseId);

    List<CourseProgress> findByCourseIdAndStatus(
            Integer courseId,
            String status
    );

    boolean existsByStudentIdAndCourseId(
            Integer studentId,
            Integer courseId
    );

    long countByCourseId(Integer courseId);

    long countByCourseIdAndStatus(
            Integer courseId,
            String status
    );
}