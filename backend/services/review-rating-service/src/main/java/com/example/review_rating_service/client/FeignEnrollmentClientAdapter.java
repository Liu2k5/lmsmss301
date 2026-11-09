package com.example.review_rating_service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@Profile("!mock")
@RequiredArgsConstructor
public class FeignEnrollmentClientAdapter implements EnrollmentClient {

    private final EnrollmentFeignClient feignClient;

    @Override
    public Optional<EnrollmentStatusDto> checkEnrollment(UUID studentId, UUID courseId) {
        try {
            return Optional.ofNullable(feignClient.checkEnrollment(studentId, courseId));
        } catch (Exception e) {
            log.warn("Failed to check enrollment via Feign client: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
