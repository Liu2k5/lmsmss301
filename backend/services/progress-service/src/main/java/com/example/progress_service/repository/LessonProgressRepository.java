package com.example.progress_service.repository;

import com.example.progress_service.entity.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LessonProgressRepository
        extends JpaRepository<LessonProgress, Integer> {

    List<LessonProgress> findByCourseProgressId(Integer progressId);

    Optional<LessonProgress> findByCourseProgressIdAndLessonId(
            Integer progressId,
            Integer lessonId
    );

    List<LessonProgress> findByCourseProgressIdAndCompleted(
            Integer progressId,
            Boolean completed
    );

    long countByCourseProgressIdAndCompleted(
            Integer progressId,
            Boolean completed
    );

    boolean existsByCourseProgressIdAndLessonId(
            Integer progressId,
            Integer lessonId
    );

    void deleteByCourseProgressIdAndLessonId(
            Integer progressId,
            Integer lessonId
    );

    Optional<LessonProgress>
    findFirstByCourseProgressIdAndCompletedOrderByLessonIdAsc(
            Integer progressId,
            Boolean completed
    );

    // UC-P10: đếm bài học còn trong curriculum
    long countByCourseProgressIdAndIncludedInCurriculum(
            Integer progressId,
            Boolean includedInCurriculum
    );

    // UC-P10: đếm bài học hoàn thành và còn trong curriculum
    long countByCourseProgressIdAndCompletedAndIncludedInCurriculum(
            Integer progressId,
            Boolean completed,
            Boolean includedInCurriculum
    );

    // UC-P10: lấy bài học chưa hoàn thành và còn trong curriculum
    Optional<LessonProgress>
    findFirstByCourseProgressIdAndCompletedAndIncludedInCurriculumOrderByLessonIdAsc(
            Integer progressId,
            Boolean completed,
            Boolean includedInCurriculum
    );
}