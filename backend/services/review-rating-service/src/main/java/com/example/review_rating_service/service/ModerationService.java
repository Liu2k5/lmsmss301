package com.example.review_rating_service.service;

import com.example.review_rating_service.dto.ReviewReportResponse;
import com.example.review_rating_service.dto.ReviewResponse;
import com.example.review_rating_service.entity.ReportStatus;
import com.example.review_rating_service.entity.Review;
import com.example.review_rating_service.entity.ReviewReport;
import com.example.review_rating_service.entity.ReviewStatus;
import com.example.review_rating_service.exception.ResourceNotFoundException;
import com.example.review_rating_service.repository.ReviewReportRepository;
import com.example.review_rating_service.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModerationService {

    private final ReviewRepository reviewRepository;
    private final ReviewReportRepository reportRepository;
    private final RatingAggregationService aggregationService;

    @Transactional
    public ReviewResponse updateReviewStatus(Long reviewId, ReviewStatus status) {
        Review review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá với mã: " + reviewId));

        review.setStatus(status);
        Review saved = reviewRepository.save(review);

        // Recalculate summary so hidden reviews are excluded from course averages
        aggregationService.recalculateSummary(review.getCourseId());
        log.info("Admin updated review {} status to {}", reviewId, status);

        return ReviewResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Page<ReviewReportResponse> getReports(ReportStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        ReportStatus targetStatus = status != null ? status : ReportStatus.PENDING;
        Page<ReviewReport> reports = reportRepository.findByStatusOrderByCreatedAtDesc(targetStatus, pageable);
        return reports.map(ReviewReportResponse::fromEntity);
    }

    @Transactional
    public ReviewReportResponse updateReportStatus(Long reportId, ReportStatus newStatus) {
        ReviewReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy báo cáo với mã: " + reportId));

        report.setStatus(newStatus);
        ReviewReport saved = reportRepository.save(report);
        return ReviewReportResponse.fromEntity(saved);
    }
}
