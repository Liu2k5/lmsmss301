package com.example.progress_service.repository;

import com.example.progress_service.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizAttemptRepository
        extends JpaRepository<QuizAttempt, Integer> {

    List<QuizAttempt> findByCourseProgressId(Integer progressId);

    List<QuizAttempt> findByCourseProgressIdAndQuizId(
            Integer progressId,
            Integer quizId
    );

    List<QuizAttempt> findByCourseProgressIdAndPassed(
            Integer progressId,
            Boolean passed
    );

    int countByCourseProgressIdAndQuizId(
            Integer progressId,
            Integer quizId
    );
}