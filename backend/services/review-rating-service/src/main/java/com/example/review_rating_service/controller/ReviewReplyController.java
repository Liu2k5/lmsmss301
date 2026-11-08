package com.example.review_rating_service.controller;

import com.example.review_rating_service.dto.CreateReplyRequest;
import com.example.review_rating_service.dto.ReviewReplyResponse;
import com.example.review_rating_service.security.SecurityUtils;
import com.example.review_rating_service.service.ReviewInteractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ReviewReplyController {

    private final ReviewInteractionService interactionService;

    /** UC-R06: Giảng viên phản hồi đánh giá của học viên */
    @PostMapping("/api/v1/reviews/{reviewId}/replies")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ReviewReplyResponse> replyToReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody CreateReplyRequest request) {

        UUID instructorId = SecurityUtils.getCurrentUserId();
        ReviewReplyResponse response = interactionService.replyToReview(reviewId, instructorId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
