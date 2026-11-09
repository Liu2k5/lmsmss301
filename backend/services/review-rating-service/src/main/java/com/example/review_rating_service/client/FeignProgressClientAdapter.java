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
public class FeignProgressClientAdapter implements ProgressClient {

    private final ProgressFeignClient feignClient;

    @Override
    public Optional<ProgressStatusDto> getProgress(UUID studentId, UUID courseId) {
        try {
            return Optional.ofNullable(feignClient.getCourseProgress(courseId, studentId));
        } catch (Exception e) {
            log.warn("Failed to check progress via Feign client: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
