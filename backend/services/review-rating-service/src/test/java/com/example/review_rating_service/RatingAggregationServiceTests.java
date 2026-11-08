package com.example.review_rating_service;

import com.example.review_rating_service.entity.CourseRatingSummary;
import com.example.review_rating_service.entity.Review;
import com.example.review_rating_service.entity.ReviewStatus;
import com.example.review_rating_service.repository.ReviewRepository;
import com.example.review_rating_service.service.RatingAggregationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("mock")
@Transactional
class RatingAggregationServiceTests {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private RatingAggregationService aggregationService;

    @Test
    @DisplayName("Tính toán rating summary khi có 3 reviews: 5⭐, 4⭐, 3⭐ -> Trung bình 4.00")
    void testRecalculateSummaryWithThreeReviews() {
        Integer courseId = 999;
        reviewRepository.save(Review.builder().courseId(courseId).studentId(UUID.randomUUID()).rating(5).status(ReviewStatus.APPROVED).build());
        reviewRepository.save(Review.builder().courseId(courseId).studentId(UUID.randomUUID()).rating(4).status(ReviewStatus.APPROVED).build());
        reviewRepository.save(Review.builder().courseId(courseId).studentId(UUID.randomUUID()).rating(3).status(ReviewStatus.APPROVED).build());

        CourseRatingSummary summary = aggregationService.recalculateSummary(courseId);

        assertThat(summary.getCourseId()).isEqualTo(courseId);
        assertThat(summary.getTotalReviews()).isEqualTo(3);
        assertThat(summary.getAverageRating()).isEqualByComparingTo("4.00");
        assertThat(summary.getStar5Count()).isEqualTo(1);
        assertThat(summary.getStar4Count()).isEqualTo(1);
        assertThat(summary.getStar3Count()).isEqualTo(1);
        assertThat(summary.getStar2Count()).isEqualTo(0);
        assertThat(summary.getStar1Count()).isEqualTo(0);
    }

    @Test
    @DisplayName("Tính toán rating summary khi chưa có review nào -> Điểm 0.00, tổng 0")
    void testRecalculateSummaryWithZeroReviews() {
        Integer courseId = 888;
        CourseRatingSummary summary = aggregationService.recalculateSummary(courseId);

        assertThat(summary.getTotalReviews()).isEqualTo(0);
        assertThat(summary.getAverageRating()).isEqualByComparingTo("0.00");
        assertThat(summary.getStar5Count()).isEqualTo(0);
    }
}
