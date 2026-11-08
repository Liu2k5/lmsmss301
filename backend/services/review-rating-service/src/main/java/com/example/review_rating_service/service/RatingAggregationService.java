package com.example.review_rating_service.service;

import com.example.review_rating_service.entity.CourseRatingSummary;
import com.example.review_rating_service.entity.Review;
import com.example.review_rating_service.entity.ReviewStatus;
import com.example.review_rating_service.repository.CourseRatingSummaryRepository;
import com.example.review_rating_service.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RatingAggregationService {

    private final ReviewRepository reviewRepository;
    private final CourseRatingSummaryRepository summaryRepository;

    @Transactional
    public CourseRatingSummary recalculateSummary(Integer courseId) {
        List<Review> approvedReviews = reviewRepository
                .findByCourseIdAndStatusAndDeletedAtIsNull(courseId, ReviewStatus.APPROVED);

        int total = approvedReviews.size();
        int s1 = 0, s2 = 0, s3 = 0, s4 = 0, s5 = 0;
        int sumPoints = 0;

        for (Review r : approvedReviews) {
            int rating = r.getRating() != null ? r.getRating() : 0;
            sumPoints += rating;
            switch (rating) {
                case 1 -> s1++;
                case 2 -> s2++;
                case 3 -> s3++;
                case 4 -> s4++;
                case 5 -> s5++;
                default -> {}
            }
        }

        BigDecimal average = BigDecimal.ZERO;
        if (total > 0) {
            average = BigDecimal.valueOf((double) sumPoints / total)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        CourseRatingSummary summary = summaryRepository.findByCourseId(courseId)
                .orElseGet(() -> CourseRatingSummary.builder().courseId(courseId).build());

        summary.setTotalReviews(total);
        summary.setAverageRating(average);
        summary.setStar1Count(s1);
        summary.setStar2Count(s2);
        summary.setStar3Count(s3);
        summary.setStar4Count(s4);
        summary.setStar5Count(s5);

        log.info("Recalculated rating summary for courseId={}: avg={}, total={}", courseId, average, total);
        return summaryRepository.save(summary);
    }

    @Transactional(readOnly = true)
    public CourseRatingSummary getSummaryByCourseId(Integer courseId) {
        return summaryRepository.findByCourseId(courseId)
                .orElseGet(() -> CourseRatingSummary.builder()
                        .courseId(courseId)
                        .averageRating(BigDecimal.ZERO)
                        .totalReviews(0)
                        .star1Count(0)
                        .star2Count(0)
                        .star3Count(0)
                        .star4Count(0)
                        .star5Count(0)
                        .build());
    }
}
