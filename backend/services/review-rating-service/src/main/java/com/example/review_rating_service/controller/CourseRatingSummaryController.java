package com.example.review_rating_service.controller;

import com.example.review_rating_service.dto.CourseRatingSummaryResponse;
import com.example.review_rating_service.entity.CourseRatingSummary;
import com.example.review_rating_service.service.RatingAggregationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CourseRatingSummaryController {

    private final RatingAggregationService aggregationService;

    /** UC-R05: Xem tổng hợp điểm đánh giá & phân bố sao của khóa học */
    @GetMapping("/api/v1/courses/{courseId}/ratings/summary")
    public ResponseEntity<CourseRatingSummaryResponse> getCourseRatingSummary(@PathVariable Integer courseId) {
        CourseRatingSummary summary = aggregationService.getSummaryByCourseId(courseId);
        return ResponseEntity.ok(CourseRatingSummaryResponse.fromEntity(summary));
    }
}
