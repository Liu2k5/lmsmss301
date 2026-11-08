package com.example.progress_service.repository;

import com.example.progress_service.entity.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface LessonProgressRepository
        extends JpaRepository<LessonProgress, UUID> {

    List<LessonProgress> findByCourseProgressId(UUID progressId);

    Optional<LessonProgress> findByCourseProgressIdAndLessonId(
            UUID progressId,
            UUID lessonId
    );

    List<LessonProgress> findByCourseProgressIdAndCompleted(
            UUID progressId,
            Boolean completed
    );

    long countByCourseProgressIdAndCompleted(
            UUID progressId,
            Boolean completed
    );

    boolean existsByCourseProgressIdAndLessonId(
            UUID progressId,
            UUID lessonId
    );

    void deleteByCourseProgressIdAndLessonId(
            UUID progressId,
            UUID lessonId
    );

    Optional<LessonProgress>
    findFirstByCourseProgressIdAndCompletedOrderByLessonIdAsc(
            UUID progressId,
            Boolean completed
    );

    // UC-P10: đếm bài học còn trong curriculum
    long countByCourseProgressIdAndIncludedInCurriculum(
            UUID progressId,
            Boolean includedInCurriculum
    );

    // UC-P10: đếm bài học hoàn thành và còn trong curriculum
    long countByCourseProgressIdAndCompletedAndIncludedInCurriculum(
            UUID progressId,
            Boolean completed,
            Boolean includedInCurriculum
    );

    // UC-P10: lấy bài học chưa hoàn thành và còn trong curriculum
    Optional<LessonProgress>
    findFirstByCourseProgressIdAndCompletedAndIncludedInCurriculumOrderByLessonIdAsc(
            UUID progressId,
            Boolean completed,
            Boolean includedInCurriculum
    );
}