package com.example.review_rating_service.dto;

import com.example.review_rating_service.entity.CourseRatingSummary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseRatingSummaryResponse {
    private Integer courseId;
    private BigDecimal averageRating;
    private Integer totalReviews;
    private Integer star5Count;
    private Integer star4Count;
    private Integer star3Count;
    private Integer star2Count;
    private Integer star1Count;
    private Map<Integer, Double> starPercentages;

    public static CourseRatingSummaryResponse fromEntity(CourseRatingSummary summary) {
        int total = summary.getTotalReviews() != null ? summary.getTotalReviews() : 0;
        Map<Integer, Double> percentages = new HashMap<>();

        if (total > 0) {
            percentages.put(5, roundPercentage((double) summary.getStar5Count() * 100.0 / total));
            percentages.put(4, roundPercentage((double) summary.getStar4Count() * 100.0 / total));
            percentages.put(3, roundPercentage((double) summary.getStar3Count() * 100.0 / total));
            percentages.put(2, roundPercentage((double) summary.getStar2Count() * 100.0 / total));
            percentages.put(1, roundPercentage((double) summary.getStar1Count() * 100.0 / total));
        } else {
            percentages.put(5, 0.0);
            percentages.put(4, 0.0);
            percentages.put(3, 0.0);
            percentages.put(2, 0.0);
            percentages.put(1, 0.0);
        }

        return CourseRatingSummaryResponse.builder()
                .courseId(summary.getCourseId())
                .averageRating(summary.getAverageRating() != null ? summary.getAverageRating() : BigDecimal.ZERO)
                .totalReviews(total)
                .star5Count(summary.getStar5Count())
                .star4Count(summary.getStar4Count())
                .star3Count(summary.getStar3Count())
                .star2Count(summary.getStar2Count())
                .star1Count(summary.getStar1Count())
                .starPercentages(percentages)
                .build();
    }

    private static double roundPercentage(double val) {
        return BigDecimal.valueOf(val).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
