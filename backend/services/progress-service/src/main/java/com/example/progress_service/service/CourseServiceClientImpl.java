package com.example.progress_service.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class CourseServiceClientImpl
        implements CourseServiceClient {

    @Override
    public List<UUID> getRequiredQuizIds(UUID courseId) {

        // TODO: Thay bằng API gọi Course Service.
        // Tạm thời giả định khóa học không có quiz bắt buộc.
        return List.of();
    }
}