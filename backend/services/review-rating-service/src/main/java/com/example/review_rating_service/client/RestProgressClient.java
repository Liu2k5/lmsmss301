package com.example.review_rating_service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@Profile("!mock")
public class RestProgressClient implements ProgressClient {

    private final RestClient restClient;

    public RestProgressClient(@Value("${app.services.progress.url:http://localhost:8084}") String progressServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(progressServiceUrl)
                .build();
    }

    @Override
    public Optional<ProgressStatusDto> getProgress(UUID studentId, UUID courseId) {
        try {
            return Optional.ofNullable(
                    restClient.get()
                            .uri("/api/v1/progress/{studentId}/{courseId}", studentId, courseId)
                            .retrieve()
                            .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                                log.warn("Progress service returned 4xx status for studentId={}, courseId={}", studentId, courseId);
                            })
                            .body(ProgressStatusDto.class)
            );
        } catch (Exception e) {
            log.warn("Failed to check progress via Progress Service: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
