package com.example.progress_service.controller;

import java.util.UUID;
import com.example.progress_service.dto.CourseAnalyticsResponse;
import com.example.progress_service.service.ProgressAnalyticsService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class ProgressAnalyticsController {

    private final ProgressAnalyticsService analyticsService;

    /**
     * Thống kê một khóa học.
     */
    @GetMapping("/courses/{courseId}")
    public CourseAnalyticsResponse getCourseAnalytics(
            @PathVariable UUID courseId) {

        return analyticsService.getCourseAnalytics(courseId);
    }
}