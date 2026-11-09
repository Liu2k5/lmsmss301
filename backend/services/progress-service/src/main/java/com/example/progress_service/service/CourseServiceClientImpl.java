package com.example.progress_service.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.example.progress_service.dto.QuizAnswerKey;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CourseServiceClientImpl implements CourseServiceClient {

    private final RestClient rest;

    public CourseServiceClientImpl(
            @Value("${app.services.course.url:http://localhost:8081}") String courseServiceUrl) {
        this.rest = RestClient.builder().baseUrl(courseServiceUrl).build();
    }

    @Override
    public List<UUID> getRequiredQuizIds(UUID courseId) {

        // TODO: Thay bằng API gọi Course Service.
        // Tạm thời giả định khóa học không có quiz bắt buộc.
        return List.of();
    }

    /**
     * UC-P04: lấy đề + đáp án đúng để chấm bài. Đây là API nội bộ của Course Service
     * (`/internal/quizzes/{id}/answer-key`), không đi qua Gateway.
     */
    @Override
    public Optional<QuizAnswerKey> fetchAnswerKey(UUID quizId) {
        try {
            QuizAnswerKey key = rest.get()
                    .uri("/internal/quizzes/{quizId}/answer-key", quizId)
                    .retrieve()
                    .body(QuizAnswerKey.class);
            return Optional.ofNullable(key);
        }
        catch (RuntimeException ex) {
            log.warn("Không lấy được đáp án quiz {} từ Course Service: {}", quizId, ex.getMessage());
            return Optional.empty();
        }
    }
}
