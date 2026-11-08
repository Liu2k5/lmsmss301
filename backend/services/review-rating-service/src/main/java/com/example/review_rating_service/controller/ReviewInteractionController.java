package com.example.review_rating_service.controller;

import com.example.review_rating_service.dto.HelpfulVoteResponse;
import com.example.review_rating_service.dto.ReportReviewRequest;
import com.example.review_rating_service.dto.ReviewReportResponse;
import com.example.review_rating_service.security.SecurityUtils;
import com.example.review_rating_service.service.ReviewInteractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ReviewInteractionController {

    private final ReviewInteractionService interactionService;

    /** UC-R07: Đánh dấu nhận xét hữu ích (Like / Helpful vote - bấm lại để hủy) */
    @PostMapping("/api/v1/reviews/{reviewId}/helpful")
    public ResponseEntity<HelpfulVoteResponse> toggleHelpful(@PathVariable Long reviewId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        HelpfulVoteResponse response = interactionService.toggleHelpful(reviewId, userId);
        return ResponseEntity.ok(response);
    }

    /** UC-R08: Báo cáo đánh giá vi phạm (Spam, nội dung phản cảm, công kích) */
    @PostMapping("/api/v1/reviews/{reviewId}/reports")
    public ResponseEntity<ReviewReportResponse> reportReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReportReviewRequest request) {

        UUID reporterId = SecurityUtils.getCurrentUserId();
        ReviewReportResponse response = interactionService.reportReview(reviewId, reporterId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
