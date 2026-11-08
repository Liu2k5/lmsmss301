package com.example.progress_service.service;

import java.util.List;
import java.util.UUID;

public interface CourseServiceClient {

    List<UUID> getRequiredQuizIds(UUID courseId);
}