package com.example.review_rating_service.controller;

import com.example.review_rating_service.dto.*;
import com.example.review_rating_service.security.SecurityUtils;
import com.example.review_rating_service.security.UserContext;
import com.example.review_rating_service.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /** UC-R01: Tạo đánh giá & xếp hạng khóa học */
    @PostMapping("/api/v1/courses/{courseId}/reviews")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable UUID courseId,
            @Valid @RequestBody CreateReviewRequest request) {

        UUID studentId = SecurityUtils.getCurrentUserId();
        ReviewResponse response = reviewService.createReview(courseId, studentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** UC-R04: Xem danh sách đánh giá của khóa học (Phân trang, lọc sao, sắp xếp) */
    @GetMapping("/api/v1/courses/{courseId}/reviews")
    public ResponseEntity<Page<ReviewResponse>> getCourseReviews(
            @PathVariable UUID courseId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        UUID currentUserId = SecurityUtils.getCurrentUser().map(UserContext::getUserId).orElse(null);
        Page<ReviewResponse> response = reviewService.getCourseReviews(courseId, rating, sort, page, size, currentUserId);
        return ResponseEntity.ok(response);
    }

    /** UC-R02: Chỉnh sửa đánh giá của bản thân */
    @PutMapping("/api/v1/reviews/{reviewId}")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody UpdateReviewRequest request) {

        UUID currentUserId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.isAdmin();
        ReviewResponse response = reviewService.updateReview(reviewId, currentUserId, isAdmin, request);
        return ResponseEntity.ok(response);
    }

    /** UC-R03: Xóa đánh giá */
    @DeleteMapping("/api/v1/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.isAdmin();
        reviewService.deleteReview(reviewId, currentUserId, isAdmin);
        return ResponseEntity.noContent().build();
    }

    /** UC-R11: Kiểm tra quyền viết đánh giá của học viên hiện tại */
    @GetMapping("/api/v1/courses/{courseId}/reviews/eligibility")
    public ResponseEntity<ReviewEligibilityResponse> checkEligibility(@PathVariable UUID courseId) {
        UUID studentId = SecurityUtils.getCurrentUserId();
        ReviewEligibilityResponse response = reviewService.checkEligibility(courseId, studentId);
        return ResponseEntity.ok(response);
    }

    /** UC-R10: Xem lịch sử đánh giá của tôi */
    @GetMapping("/api/v1/my-reviews")
    public ResponseEntity<List<ReviewResponse>> getMyReviews() {
        UUID studentId = SecurityUtils.getCurrentUserId();
        List<ReviewResponse> response = reviewService.getMyReviews(studentId);
        return ResponseEntity.ok(response);
    }
}
