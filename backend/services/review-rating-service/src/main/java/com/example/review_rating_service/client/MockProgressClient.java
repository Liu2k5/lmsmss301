package com.example.review_rating_service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@Profile("mock")
public class MockProgressClient implements ProgressClient {

    @Override
    public Optional<ProgressStatusDto> getProgress(UUID studentId, Integer courseId) {
        log.info("[MOCK] Get progress for studentId={} and courseId={}", studentId, courseId);
        // By default in mock mode, assume student has completed the course
        return Optional.of(ProgressStatusDto.builder()
                .studentId(studentId)
                .courseId(courseId)
                .percentComplete(new BigDecimal("100.00"))
                .courseCompleted(true)
                .build());
    }
}
