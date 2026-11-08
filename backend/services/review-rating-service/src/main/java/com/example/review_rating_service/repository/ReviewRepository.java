package com.example.review_rating_service.repository;

import com.example.review_rating_service.entity.Review;
import com.example.review_rating_service.entity.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByIdAndDeletedAtIsNull(Long id);

    Optional<Review> findByCourseIdAndStudentIdAndDeletedAtIsNull(UUID courseId, UUID studentId);

    Page<Review> findByCourseIdAndStatusAndDeletedAtIsNull(UUID courseId, ReviewStatus status, Pageable pageable);

    Page<Review> findByCourseIdAndStatusAndRatingAndDeletedAtIsNull(UUID courseId, ReviewStatus status, Integer rating, Pageable pageable);

    List<Review> findByCourseIdAndStatusAndDeletedAtIsNull(UUID courseId, ReviewStatus status);

    List<Review> findByStudentIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID studentId);

    Page<Review> findByStatusAndDeletedAtIsNull(ReviewStatus status, Pageable pageable);

    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.courseId = :courseId AND r.status = :status AND r.deletedAt IS NULL GROUP BY r.rating")
    List<Object[]> countRatingsByCourseIdAndStatus(@Param("courseId") UUID courseId, @Param("status") ReviewStatus status);
}
