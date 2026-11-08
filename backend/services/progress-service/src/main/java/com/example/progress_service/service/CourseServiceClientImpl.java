package com.example.progress_service.service;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CourseServiceClientImpl
        implements CourseServiceClient {

    @Override
    public List<Integer> getRequiredQuizIds(Integer courseId) {

        // TODO: Thay bằng API gọi Course Service.
        // Tạm thời giả định khóa học không có quiz bắt buộc.
        return List.of();
    }
}