package com.example.review_rating_service.service;

import com.example.review_rating_service.client.EnrollmentClient;
import com.example.review_rating_service.client.EnrollmentStatusDto;
import com.example.review_rating_service.client.ProgressClient;
import com.example.review_rating_service.client.ProgressStatusDto;
import com.example.review_rating_service.dto.*;
import com.example.review_rating_service.entity.Review;
import com.example.review_rating_service.entity.ReviewPermission;
import com.example.review_rating_service.entity.ReviewStatus;
import com.example.review_rating_service.exception.ConflictException;
import com.example.review_rating_service.exception.ForbiddenException;
import com.example.review_rating_service.exception.ResourceNotFoundException;
import com.example.review_rating_service.security.SecurityUtils;
import com.example.review_rating_service.repository.ReviewHelpfulVoteRepository;
import com.example.review_rating_service.repository.ReviewPermissionRepository;
import com.example.review_rating_service.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewHelpfulVoteRepository helpfulVoteRepository;
    private final ReviewPermissionRepository permissionRepository;
    private final RatingAggregationService aggregationService;
    private final EnrollmentClient enrollmentClient;
    private final ProgressClient progressClient;

    @Value("${app.review.require-enrollment:true}")
    private boolean requireEnrollment;

    @Value("${app.review.require-completed-course:false}")
    private boolean requireCompletedCourse;

    @Transactional
    public ReviewEligibilityResponse checkEligibility(UUID courseId, UUID studentId) {
        // 1. Kiểm tra học viên đã viết review trước đó chưa
        Optional<Review> existingReview = reviewRepository
                .findByCourseIdAndStudentIdAndDeletedAtIsNull(courseId, studentId);
        if (existingReview.isPresent()) {
            return ReviewEligibilityResponse.builder()
                    .canReview(false)
                    .reason("Bạn đã gửi đánh giá cho khóa học này rồi")
                    .existingReviewId(existingReview.get().getId())
                    .build();
        }

        // 2. Guard: kiểm tra đã ghi danh khóa học chưa (Enrollment Service qua OpenFeign)
        if (requireEnrollment) {
            Optional<EnrollmentStatusDto> enrollmentOpt = enrollmentClient.checkEnrollment(studentId, courseId);
            if (enrollmentOpt.isEmpty() || !enrollmentOpt.get().isActive()) {
                return ReviewEligibilityResponse.builder()
                        .canReview(false)
                        .reason("Bạn cần đăng ký khóa học để có thể viết đánh giá")
                        .build();
            }
        }

        // 3. Guard: kiểm tra đã hoàn thành khóa học chưa (Hybrid: DB cache + Progress Service qua OpenFeign)
        // Admin bỏ qua: admin không có tiến độ học nhưng vẫn cần kiểm tra quyền để xử lý kiểm duyệt.
        if (requireCompletedCourse && !SecurityUtils.isAdmin()) {
            boolean hasPermission = permissionRepository.existsByCourseIdAndStudentId(courseId, studentId);
            if (!hasPermission) {
                Optional<ProgressStatusDto> progressOpt = progressClient.getProgress(studentId, courseId);
                if (progressOpt.isEmpty() || !progressOpt.get().isCompleted()) {
                    return ReviewEligibilityResponse.builder()
                            .canReview(false)
                            .reason("Bạn cần hoàn thành khóa học để có thể gửi đánh giá")
                            .build();
                } else {
                    // Tự động lưu cache quyền vào DB cho các lần sau
                    grantReviewPermission(studentId, courseId);
                }
            }
        }

        return ReviewEligibilityResponse.builder()
                .canReview(true)
                .reason("Hợp lệ để viết đánh giá")
                .build();
    }

    @Transactional
    public ReviewResponse createReview(UUID courseId, UUID studentId, CreateReviewRequest request) {
        ReviewEligibilityResponse eligibility = checkEligibility(courseId, studentId);
        if (!eligibility.isCanReview()) {
            if (eligibility.getExistingReviewId() != null) {
                throw new ConflictException(eligibility.getReason());
            }
            throw new ForbiddenException(eligibility.getReason());
        }

        Review review = Review.builder()
                .courseId(courseId)
                .studentId(studentId)
                .rating(request.getRating())
                .comment(request.getComment() != null ? request.getComment().trim() : null)
                .status(ReviewStatus.APPROVED)
                .helpfulCount(0)
                .build();

        Review savedReview = reviewRepository.save(review);
        aggregationService.recalculateSummary(courseId);

        log.info("Student {} created review {} for course {}", studentId, savedReview.getId(), courseId);
        return ReviewResponse.fromEntity(savedReview, false);
    }

    @Transactional
    public ReviewResponse updateReview(Long reviewId, UUID currentUserId, boolean isAdmin, UpdateReviewRequest request) {
        Review review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá với mã: " + reviewId));

        if (!isAdmin && !review.getStudentId().equals(currentUserId)) {
            throw new ForbiddenException("Bạn không có quyền sửa đánh giá này");
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment() != null ? request.getComment().trim() : null);

        Review updatedReview = reviewRepository.save(review);
        aggregationService.recalculateSummary(review.getCourseId());

        boolean isHelpfulByMe = helpfulVoteRepository.existsByReviewIdAndUserId(reviewId, currentUserId);
        return ReviewResponse.fromEntity(updatedReview, isHelpfulByMe);
    }

    @Transactional
    public void deleteReview(Long reviewId, UUID currentUserId, boolean isAdmin) {
        Review review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá với mã: " + reviewId));

        if (!isAdmin && !review.getStudentId().equals(currentUserId)) {
            throw new ForbiddenException("Bạn không có quyền xóa đánh giá này");
        }

        review.setDeletedAt(LocalDateTime.now());
        reviewRepository.save(review);

        aggregationService.recalculateSummary(review.getCourseId());
        log.info("Review {} soft-deleted by user {}", reviewId, currentUserId);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getCourseReviews(UUID courseId,
                                                 Integer ratingFilter,
                                                 String sortBy,
                                                 int page,
                                                 int size,
                                                 UUID currentUserId) {
        Sort sort = switch (sortBy != null ? sortBy.toLowerCase() : "newest") {
            case "highest_rating" -> Sort.by(Sort.Direction.DESC, "rating").and(Sort.by(Sort.Direction.DESC, "createdAt"));
            case "lowest_rating" -> Sort.by(Sort.Direction.ASC, "rating").and(Sort.by(Sort.Direction.DESC, "createdAt"));
            case "most_helpful" -> Sort.by(Sort.Direction.DESC, "helpfulCount").and(Sort.by(Sort.Direction.DESC, "createdAt"));
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sort);

        Page<Review> reviewsPage;
        if (ratingFilter != null && ratingFilter >= 1 && ratingFilter <= 5) {
            reviewsPage = reviewRepository.findByCourseIdAndStatusAndRatingAndDeletedAtIsNull(
                    courseId, ReviewStatus.APPROVED, ratingFilter, pageable);
        } else {
            reviewsPage = reviewRepository.findByCourseIdAndStatusAndDeletedAtIsNull(
                    courseId, ReviewStatus.APPROVED, pageable);
        }

        return reviewsPage.map(r -> {
            boolean isHelpful = currentUserId != null && helpfulVoteRepository.existsByReviewIdAndUserId(r.getId(), currentUserId);
            return ReviewResponse.fromEntity(r, isHelpful);
        });
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getMyReviews(UUID studentId) {
        List<Review> myReviews = reviewRepository
                .findByStudentIdAndDeletedAtIsNullOrderByCreatedAtDesc(studentId);
        return myReviews.stream()
                .map(r -> ReviewResponse.fromEntity(r, false))
                .toList();
    }

    @Transactional
    public void grantReviewPermission(UUID studentId, UUID courseId) {
        if (!permissionRepository.existsByCourseIdAndStudentId(courseId, studentId)) {
            ReviewPermission permission = ReviewPermission.builder()
                    .studentId(studentId)
                    .courseId(courseId)
                    .build();
            permissionRepository.save(permission);
            log.info("Granted review permission for studentId={} on courseId={}", studentId, courseId);
        }
    }
}
