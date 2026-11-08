package com.example.review_rating_service.dto;

import com.example.review_rating_service.entity.Review;
import com.example.review_rating_service.entity.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private Integer courseId;
    private UUID studentId;
    private Integer rating;
    private String comment;
    private ReviewStatus status;
    private Integer helpfulCount;
    private Boolean isHelpfulByMe;
    private ReviewReplyResponse reply;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReviewResponse fromEntity(Review review) {
        return fromEntity(review, null);
    }

    public static ReviewResponse fromEntity(Review review, Boolean isHelpfulByMe) {
        ReviewReplyResponse replyResponse = null;
        if (review.getReply() != null) {
            replyResponse = ReviewReplyResponse.builder()
                    .id(review.getReply().getId())
                    .instructorId(review.getReply().getInstructorId())
                    .content(review.getReply().getContent())
                    .createdAt(review.getReply().getCreatedAt())
                    .updatedAt(review.getReply().getUpdatedAt())
                    .build();
        }

        return ReviewResponse.builder()
                .id(review.getId())
                .courseId(review.getCourseId())
                .studentId(review.getStudentId())
                .rating(review.getRating())
                .comment(review.getComment())
                .status(review.getStatus())
                .helpfulCount(review.getHelpfulCount())
                .isHelpfulByMe(isHelpfulByMe)
                .reply(replyResponse)
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
