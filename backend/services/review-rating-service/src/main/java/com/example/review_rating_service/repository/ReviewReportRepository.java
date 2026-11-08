package com.example.review_rating_service.repository;

import com.example.review_rating_service.entity.ReportStatus;
import com.example.review_rating_service.entity.ReviewReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {

    List<ReviewReport> findByReviewId(Long reviewId);

    long countByReviewIdAndStatus(Long reviewId, ReportStatus status);

    boolean existsByReviewIdAndReporterId(Long reviewId, UUID reporterId);

    Page<ReviewReport> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);
}
