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
public class RestEnrollmentClient implements EnrollmentClient {

    private final RestClient restClient;

    public RestEnrollmentClient(@Value("${app.services.enrollment.url:http://localhost:8083}") String enrollmentServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(enrollmentServiceUrl)
                .build();
    }

    @Override
    public Optional<EnrollmentStatusDto> checkEnrollment(UUID studentId, Integer courseId) {
        try {
            return Optional.ofNullable(
                    restClient.get()
                            .uri("/api/v1/enrollments/check?studentId={studentId}&courseId={courseId}", studentId, courseId)
                            .retrieve()
                            .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                                log.warn("Enrollment service returned 4xx status for studentId={}, courseId={}", studentId, courseId);
                            })
                            .body(EnrollmentStatusDto.class)
            );
        } catch (Exception e) {
            log.warn("Failed to check enrollment via Enrollment Service: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
