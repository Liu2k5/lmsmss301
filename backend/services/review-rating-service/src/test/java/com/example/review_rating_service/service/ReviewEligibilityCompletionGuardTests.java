package com.example.review_rating_service.service;

import com.example.review_rating_service.client.EnrollmentClient;
import com.example.review_rating_service.client.EnrollmentStatusDto;
import com.example.review_rating_service.client.ProgressClient;
import com.example.review_rating_service.client.ProgressStatusDto;
import com.example.review_rating_service.dto.ReviewEligibilityResponse;
import com.example.review_rating_service.repository.ReviewHelpfulVoteRepository;
import com.example.review_rating_service.repository.ReviewPermissionRepository;
import com.example.review_rating_service.repository.ReviewRepository;
import com.example.review_rating_service.security.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Guard UC-R01: chỉ cho gửi đánh giá khi học viên đã hoàn thành khoá học
 * ({@code app.review.require-completed-course=true}).
 */
class ReviewEligibilityCompletionGuardTests {

    private static final UUID STUDENT = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ADMIN = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID COURSE = UUID.fromString("9c2b80e6-29ee-457b-9629-7b88fb5ae66f");

    private ReviewRepository reviewRepository;
    private ReviewPermissionRepository permissionRepository;
    private EnrollmentClient enrollmentClient;
    private ProgressClient progressClient;
    private ReviewService service;

    @BeforeEach
    void setUp() {
        reviewRepository = mock(ReviewRepository.class);
        permissionRepository = mock(ReviewPermissionRepository.class);
        enrollmentClient = mock(EnrollmentClient.class);
        progressClient = mock(ProgressClient.class);
        service = new ReviewService(
                reviewRepository,
                mock(ReviewHelpfulVoteRepository.class),
                permissionRepository,
                mock(RatingAggregationService.class),
                enrollmentClient,
                progressClient);

        ReflectionTestUtils.setField(service, "requireEnrollment", true);
        ReflectionTestUtils.setField(service, "requireCompletedCourse", true);

        when(reviewRepository.findByCourseIdAndStudentIdAndDeletedAtIsNull(COURSE, STUDENT))
                .thenReturn(Optional.empty());
        when(permissionRepository.existsByCourseIdAndStudentId(COURSE, STUDENT)).thenReturn(false);
        when(enrollmentClient.checkEnrollment(any(), any())).thenReturn(Optional.of(
                EnrollmentStatusDto.builder().status("ACTIVE").build()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(UUID userId, String role) {
        UserContext principal = UserContext.builder().userId(userId).role(role).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @Test
    @DisplayName("Hoàn thành 100% khoá học → được gửi đánh giá")
    void completedCourseCanReview() {
        authenticate(STUDENT, "STUDENT");
        when(progressClient.getProgress(STUDENT, COURSE)).thenReturn(Optional.of(
                ProgressStatusDto.builder()
                        .studentId(STUDENT)
                        .courseId(COURSE)
                        .percentComplete(new BigDecimal("100.00"))
                        .courseCompleted(true)
                        .build()));

        ReviewEligibilityResponse response = service.checkEligibility(COURSE, STUDENT);

        assertThat(response.isCanReview()).isTrue();
    }

    @Test
    @DisplayName("Chưa hoàn thành khoá học → không được gửi đánh giá")
    void unfinishedCourseCannotReview() {
        authenticate(STUDENT, "STUDENT");
        when(progressClient.getProgress(STUDENT, COURSE)).thenReturn(Optional.of(
                ProgressStatusDto.builder()
                        .studentId(STUDENT)
                        .courseId(COURSE)
                        .percentComplete(new BigDecimal("25.00"))
                        .courseCompleted(false)
                        .build()));

        ReviewEligibilityResponse response = service.checkEligibility(COURSE, STUDENT);

        assertThat(response.isCanReview()).isFalse();
        assertThat(response.getReason()).contains("hoàn thành");
    }

    @Test
    @DisplayName("Không lấy được tiến độ (Progress Service lỗi) → chặn đánh giá")
    void missingProgressCannotReview() {
        authenticate(STUDENT, "STUDENT");
        when(progressClient.getProgress(STUDENT, COURSE)).thenReturn(Optional.empty());

        ReviewEligibilityResponse response = service.checkEligibility(COURSE, STUDENT);

        assertThat(response.isCanReview()).isFalse();
    }

    @Test
    @DisplayName("Admin bỏ qua guard hoàn thành khoá học")
    void adminSkipsCompletionGuard() {
        authenticate(ADMIN, "ADMIN");

        ReviewEligibilityResponse response = service.checkEligibility(COURSE, ADMIN);

        assertThat(response.isCanReview()).isTrue();
    }
}
