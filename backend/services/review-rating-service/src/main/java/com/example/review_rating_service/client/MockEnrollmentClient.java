package com.example.review_rating_service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@Profile("mock")
public class MockEnrollmentClient implements EnrollmentClient {

    @Override
    public Optional<EnrollmentStatusDto> checkEnrollment(UUID studentId, Integer courseId) {
        log.info("[MOCK] Check enrollment for studentId={} and courseId={}", studentId, courseId);
        // By default in mock mode, assume student is enrolled
        return Optional.of(EnrollmentStatusDto.builder()
                .status("ACTIVE")
                .enrolledAt(LocalDateTime.now().minusDays(10))
                .build());
    }
}
