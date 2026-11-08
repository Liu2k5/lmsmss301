package com.example.review_rating_service.repository;

import com.example.review_rating_service.entity.ReviewHelpfulVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewHelpfulVoteRepository extends JpaRepository<ReviewHelpfulVote, Long> {

    Optional<ReviewHelpfulVote> findByReviewIdAndUserId(Long reviewId, UUID userId);

    boolean existsByReviewIdAndUserId(Long reviewId, UUID userId);

    long countByReviewId(Long reviewId);
}
