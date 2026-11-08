package com.example.progress_service.repository;

import com.example.progress_service.entity.ArchivedProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ArchivedProgressRepository
        extends JpaRepository<ArchivedProgress, UUID> {

    List<ArchivedProgress> findByStudentId(UUID studentId);

    List<ArchivedProgress> findByStudentIdAndCourseId(
            UUID studentId,
            UUID courseId
    );
}