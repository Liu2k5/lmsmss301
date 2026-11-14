package com.example.review_rating_service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class ProgressFeignFallback implements ProgressFeignClient {

    @Override
    public ProgressStatusDto getCourseProgress(UUID courseId, UUID studentId) {
        log.warn("Fallback triggered: Progress Service is unreachable for courseId={}, studentId={}", courseId, studentId);
        return null;
    }
}
