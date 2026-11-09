package com.example.progress_service.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.example.progress_service.dto.QuizAnswerKey;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CourseServiceClientImpl implements CourseServiceClient {

    private final RestClient rest;

    private final ObjectMapper objectMapper;

    public CourseServiceClientImpl(
            @Value("${app.services.course.url:http://localhost:8081}") String courseServiceUrl,
            ObjectMapper objectMapper) {
        this.rest = RestClient.builder().baseUrl(courseServiceUrl).build();
        this.objectMapper = objectMapper;
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

    @Override
    public List<UUID> fetchPublishedLessonIds(UUID courseId) {
        try {
            String body = rest.get()
                    .uri("/api/v1/courses/{courseId}/curriculum/published", courseId)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(body);
            List<UUID> ids = new java.util.ArrayList<>();
            for (JsonNode section : root.path("sections")) {
                for (JsonNode lesson : section.path("lessons")) {
                    if (!lesson.path("id").isMissingNode()) {
                        ids.add(UUID.fromString(lesson.path("id").asString()));
                    }
                }
            }
            return ids;
        }
        catch (RuntimeException ex) {
            log.warn("Không lấy được curriculum của khoá {}: {}", courseId, ex.getMessage());
            return List.of();
        }
    }
}
