package com.example.review_rating_service.controller;

import com.example.review_rating_service.dto.ReviewReportResponse;
import com.example.review_rating_service.dto.ReviewResponse;
import com.example.review_rating_service.dto.UpdateReviewStatusRequest;
import com.example.review_rating_service.entity.ReportStatus;
import com.example.review_rating_service.service.ModerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewModerationController {

    private final ModerationService moderationService;

    /** UC-R09: Ẩn hoặc duyệt lại đánh giá (Tự động cập nhật điểm tổng hợp) */
    @PatchMapping("/reviews/{reviewId}/status")
    public ResponseEntity<ReviewResponse> updateReviewStatus(
            @PathVariable Long reviewId,
            @Valid @RequestBody UpdateReviewStatusRequest request) {

        ReviewResponse response = moderationService.updateReviewStatus(reviewId, request.getStatus());
        return ResponseEntity.ok(response);
    }

    /** Xem danh sách các báo cáo vi phạm cần xử lý */
    @GetMapping("/reports")
    public ResponseEntity<Page<ReviewReportResponse>> getReports(
            @RequestParam(required = false, defaultValue = "PENDING") ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<ReviewReportResponse> response = moderationService.getReports(status, page, size);
        return ResponseEntity.ok(response);
    }

    /** Cập nhật trạng thái báo cáo (RESOLVED, DISMISSED) */
    @PatchMapping("/reports/{reportId}/status")
    public ResponseEntity<ReviewReportResponse> updateReportStatus(
            @PathVariable Long reportId,
            @RequestParam ReportStatus status) {

        ReviewReportResponse response = moderationService.updateReportStatus(reportId, status);
        return ResponseEntity.ok(response);
    }
}
