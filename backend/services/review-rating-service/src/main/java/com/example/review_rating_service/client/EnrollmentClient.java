package com.example.review_rating_service.client;

import java.util.Optional;
import java.util.UUID;

public interface EnrollmentClient {
    Optional<EnrollmentStatusDto> checkEnrollment(UUID studentId, UUID courseId);
}
