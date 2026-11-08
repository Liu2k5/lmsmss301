package com.example.progress_service.controller;

import com.example.progress_service.dto.CourseAnalyticsResponse;
import com.example.progress_service.service.ProgressAnalyticsService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class ProgressAnalyticsController {

    private final ProgressAnalyticsService analyticsService;

    /**
     * Thống kê một khóa học.
     */
    @GetMapping("/courses/{courseId}")
    public CourseAnalyticsResponse getCourseAnalytics(
            @PathVariable Integer courseId) {

        return analyticsService.getCourseAnalytics(courseId);
    }
}