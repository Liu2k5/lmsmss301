package com.example.review_rating_service.client;

import java.util.Optional;
import java.util.UUID;

public interface ProgressClient {
    Optional<ProgressStatusDto> getProgress(UUID studentId, Integer courseId);
}
