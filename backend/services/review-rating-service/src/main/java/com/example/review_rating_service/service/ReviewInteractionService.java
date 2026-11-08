package com.example.review_rating_service.service;

import com.example.review_rating_service.dto.CreateReplyRequest;
import com.example.review_rating_service.dto.HelpfulVoteResponse;
import com.example.review_rating_service.dto.ReportReviewRequest;
import com.example.review_rating_service.dto.ReviewReplyResponse;
import com.example.review_rating_service.dto.ReviewReportResponse;
import com.example.review_rating_service.entity.*;
import com.example.review_rating_service.exception.ConflictException;
import com.example.review_rating_service.exception.ResourceNotFoundException;
import com.example.review_rating_service.repository.ReviewHelpfulVoteRepository;
import com.example.review_rating_service.repository.ReviewReplyRepository;
import com.example.review_rating_service.repository.ReviewReportRepository;
import com.example.review_rating_service.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewInteractionService {

    private final ReviewRepository reviewRepository;
    private final ReviewHelpfulVoteRepository helpfulVoteRepository;
    private final ReviewReportRepository reportRepository;
    private final ReviewReplyRepository replyRepository;

    @Value("${app.review.report-threshold-to-hide:5}")
    private int reportThresholdToHide;

    @Transactional
    public HelpfulVoteResponse toggleHelpful(Long reviewId, UUID userId) {
        Review review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá với mã: " + reviewId));

        Optional<ReviewHelpfulVote> existingVote = helpfulVoteRepository.findByReviewIdAndUserId(reviewId, userId);

        if (existingVote.isPresent()) {
            helpfulVoteRepository.delete(existingVote.get());
            int newCount = Math.max(0, review.getHelpfulCount() - 1);
            review.setHelpfulCount(newCount);
            reviewRepository.save(review);
            log.info("User {} un-voted helpful for review {}", userId, reviewId);
            return HelpfulVoteResponse.builder()
                    .voted(false)
                    .helpfulCount(newCount)
                    .build();
        } else {
            ReviewHelpfulVote vote = ReviewHelpfulVote.builder()
                    .review(review)
                    .userId(userId)
                    .build();
            helpfulVoteRepository.save(vote);
            int newCount = review.getHelpfulCount() + 1;
            review.setHelpfulCount(newCount);
            reviewRepository.save(review);
            log.info("User {} voted helpful for review {}", userId, reviewId);
            return HelpfulVoteResponse.builder()
                    .voted(true)
                    .helpfulCount(newCount)
                    .build();
        }
    }

    @Transactional
    public ReviewReportResponse reportReview(Long reviewId, UUID reporterId, ReportReviewRequest request) {
        Review review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá với mã: " + reviewId));

        if (reportRepository.existsByReviewIdAndReporterId(reviewId, reporterId)) {
            throw new ConflictException("Bạn đã gửi báo cáo vi phạm cho đánh giá này rồi");
        }

        ReviewReport report = ReviewReport.builder()
                .review(review)
                .reporterId(reporterId)
                .reason(request.getReason().trim())
                .status(ReportStatus.PENDING)
                .build();

        ReviewReport savedReport = reportRepository.save(report);

        long pendingCount = reportRepository.countByReviewIdAndStatus(reviewId, ReportStatus.PENDING);
        if (pendingCount >= reportThresholdToHide) {
            review.setStatus(ReviewStatus.REPORTED);
            reviewRepository.save(review);
            log.warn("Review {} flagged as REPORTED due to {} reports", reviewId, pendingCount);
        }

        return ReviewReportResponse.fromEntity(savedReport);
    }

    @Transactional
    public ReviewReplyResponse replyToReview(Long reviewId, UUID instructorId, CreateReplyRequest request) {
        Review review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá với mã: " + reviewId));

        ReviewReply reply = replyRepository.findByReviewId(reviewId)
                .orElseGet(() -> ReviewReply.builder()
                        .review(review)
                        .instructorId(instructorId)
                        .build());

        reply.setContent(request.getContent().trim());
        reply.setInstructorId(instructorId);

        ReviewReply savedReply = replyRepository.save(reply);
        review.setReply(savedReply);
        reviewRepository.save(review);

        log.info("Instructor {} replied to review {}", instructorId, reviewId);
        return ReviewReplyResponse.builder()
                .id(savedReply.getId())
                .instructorId(savedReply.getInstructorId())
                .content(savedReply.getContent())
                .createdAt(savedReply.getCreatedAt())
                .updatedAt(savedReply.getUpdatedAt())
                .build();
    }
}
