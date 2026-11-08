package com.example.review_rating_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "course_rating_summaries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseRatingSummary {

    @Id
    @Column(name = "course_id", nullable = false)
    private Integer courseId;

    @Column(name = "average_rating", precision = 3, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "total_reviews", nullable = false)
    @Builder.Default
    private Integer totalReviews = 0;

    @Column(name = "star_5_count", nullable = false)
    @Builder.Default
    private Integer star5Count = 0;

    @Column(name = "star_4_count", nullable = false)
    @Builder.Default
    private Integer star4Count = 0;

    @Column(name = "star_3_count", nullable = false)
    @Builder.Default
    private Integer star3Count = 0;

    @Column(name = "star_2_count", nullable = false)
    @Builder.Default
    private Integer star2Count = 0;

    @Column(name = "star_1_count", nullable = false)
    @Builder.Default
    private Integer star1Count = 0;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
