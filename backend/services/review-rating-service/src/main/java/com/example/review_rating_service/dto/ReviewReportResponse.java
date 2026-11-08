package com.example.review_rating_service.dto;

import com.example.review_rating_service.entity.ReportStatus;
import com.example.review_rating_service.entity.ReviewReport;
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
public class ReviewReportResponse {
    private Long id;
    private Long reviewId;
    private UUID reporterId;
    private String reason;
    private ReportStatus status;
    private LocalDateTime createdAt;

    public static ReviewReportResponse fromEntity(ReviewReport report) {
        return ReviewReportResponse.builder()
                .id(report.getId())
                .reviewId(report.getReview().getId())
                .reporterId(report.getReporterId())
                .reason(report.getReason())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
