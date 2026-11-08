package com.example.review_rating_service;

import com.example.review_rating_service.entity.*;
import com.example.review_rating_service.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ReviewRepositoryTests {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private CourseRatingSummaryRepository summaryRepository;

    @Autowired
    private ReviewHelpfulVoteRepository helpfulVoteRepository;

    @Test
    @DisplayName("Lưu và tìm kiếm review hoạt động (chưa bị soft delete)")
    void testSaveAndFindReview() {
        UUID studentId = UUID.randomUUID();
        Review review = Review.builder()
                .courseId(10)
                .studentId(studentId)
                .rating(5)
                .comment("Khóa học xuất sắc!")
                .status(ReviewStatus.APPROVED)
                .helpfulCount(0)
                .build();

        Review saved = em.persistAndFlush(review);
        em.clear();

        Optional<Review> found = reviewRepository.findByIdAndDeletedAtIsNull(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getRating()).isEqualTo(5);
        assertThat(found.get().getComment()).isEqualTo("Khóa học xuất sắc!");
        assertThat(found.get().getStatus()).isEqualTo(ReviewStatus.APPROVED);
        assertThat(found.get().getStudentId()).isEqualTo(studentId);
    }

    @Test
    @DisplayName("Soft delete: review có deletedAt không được trả về bởi findByIdAndDeletedAtIsNull")
    void testSoftDeleteReview() {
        Review review = em.persistAndFlush(Review.builder()
                .courseId(10)
                .studentId(UUID.randomUUID())
                .rating(4)
                .comment("Nội dung hay")
                .status(ReviewStatus.APPROVED)
                .helpfulCount(0)
                .deletedAt(LocalDateTime.now())
                .build());
        em.clear();

        Optional<Review> found = reviewRepository.findByIdAndDeletedAtIsNull(review.getId());
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Tìm kiếm phân trang review theo courseId và status APPROVED")
    void testFindApprovedReviewsPaged() {
        Review r1 = Review.builder().courseId(10).studentId(UUID.randomUUID()).rating(5).status(ReviewStatus.APPROVED).build();
        Review r2 = Review.builder().courseId(10).studentId(UUID.randomUUID()).rating(4).status(ReviewStatus.APPROVED).build();
        Review r3 = Review.builder().courseId(10).studentId(UUID.randomUUID()).rating(1).status(ReviewStatus.HIDDEN).build();
        Review r4 = Review.builder().courseId(11).studentId(UUID.randomUUID()).rating(5).status(ReviewStatus.APPROVED).build();

        em.persist(r1);
        em.persist(r2);
        em.persist(r3);
        em.persist(r4);
        em.flush();
        em.clear();

        Page<Review> page = reviewRepository.findByCourseIdAndStatusAndDeletedAtIsNull(
                10, ReviewStatus.APPROVED, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Thống kê rating counts theo khóa học và status")
    void testCountRatingsByCourseIdAndStatus() {
        em.persist(Review.builder().courseId(20).studentId(UUID.randomUUID()).rating(5).status(ReviewStatus.APPROVED).build());
        em.persist(Review.builder().courseId(20).studentId(UUID.randomUUID()).rating(5).status(ReviewStatus.APPROVED).build());
        em.persist(Review.builder().courseId(20).studentId(UUID.randomUUID()).rating(4).status(ReviewStatus.APPROVED).build());
        em.flush();
        em.clear();

        List<Object[]> counts = reviewRepository.countRatingsByCourseIdAndStatus(20, ReviewStatus.APPROVED);
        assertThat(counts).hasSize(2);
    }

    @Test
    @DisplayName("Lưu và cập nhật CourseRatingSummary")
    void testCourseRatingSummaryOperations() {
        CourseRatingSummary summary = CourseRatingSummary.builder()
                .courseId(50)
                .averageRating(new BigDecimal("4.50"))
                .totalReviews(2)
                .star5Count(1)
                .star4Count(1)
                .star3Count(0)
                .star2Count(0)
                .star1Count(0)
                .build();

        summaryRepository.save(summary);
        em.flush();
        em.clear();

        Optional<CourseRatingSummary> loaded = summaryRepository.findByCourseId(50);
        assertThat(loaded).isPresent();
        assertThat(loaded.get().getAverageRating()).isEqualByComparingTo("4.50");
        assertThat(loaded.get().getTotalReviews()).isEqualTo(2);
    }

    @Test
    @DisplayName("Vote Helpful: kiểm tra vote của user cho review")
    void testHelpfulVotePersistence() {
        UUID studentId = UUID.randomUUID();
        UUID voterId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        Review review = em.persistAndFlush(Review.builder()
                .courseId(30)
                .studentId(studentId)
                .rating(5)
                .status(ReviewStatus.APPROVED)
                .helpfulCount(0)
                .build());

        ReviewHelpfulVote vote = ReviewHelpfulVote.builder()
                .review(review)
                .userId(voterId)
                .build();

        helpfulVoteRepository.save(vote);
        em.flush();
        em.clear();

        boolean exists = helpfulVoteRepository.existsByReviewIdAndUserId(review.getId(), voterId);
        assertThat(exists).isTrue();

        boolean notExists = helpfulVoteRepository.existsByReviewIdAndUserId(review.getId(), otherUserId);
        assertThat(notExists).isFalse();
    }
}
