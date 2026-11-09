package com.example.review_rating_service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class EnrollmentFeignFallback implements EnrollmentFeignClient {

    @Override
    public EnrollmentStatusDto checkEnrollment(UUID studentId, UUID courseId) {
        log.warn("Fallback triggered: Enrollment Service is unreachable for studentId={}, courseId={}", studentId, courseId);
        return null;
    }
}
