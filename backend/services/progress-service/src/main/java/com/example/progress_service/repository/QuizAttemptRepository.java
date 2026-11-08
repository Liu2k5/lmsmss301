package com.example.progress_service.repository;

import com.example.progress_service.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuizAttemptRepository
        extends JpaRepository<QuizAttempt, UUID> {

    List<QuizAttempt> findByCourseProgressId(UUID progressId);

    List<QuizAttempt> findByCourseProgressIdAndQuizId(
            UUID progressId,
            UUID quizId
    );

    List<QuizAttempt> findByCourseProgressIdAndPassed(
            UUID progressId,
            Boolean passed
    );

    int countByCourseProgressIdAndQuizId(
            UUID progressId,
            UUID quizId
    );
}