package com.example.progress_service.repository;

import com.example.progress_service.entity.ArchivedProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArchivedProgressRepository
        extends JpaRepository<ArchivedProgress, Integer> {

    List<ArchivedProgress> findByStudentId(Integer studentId);

    List<ArchivedProgress> findByStudentIdAndCourseId(
            Integer studentId,
            Integer courseId
    );
}