package com.example.review_rating_service.config;

import com.example.review_rating_service.entity.Review;
import com.example.review_rating_service.entity.ReviewReply;
import com.example.review_rating_service.entity.ReviewStatus;
import com.example.review_rating_service.repository.ReviewRepository;
import com.example.review_rating_service.service.RatingAggregationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private final ReviewRepository reviewRepository;
    private final RatingAggregationService aggregationService;

    public static final UUID STUDENT_1_UUID = UUID.fromString("11111111-1111-1111-1111-000000000101");
    public static final UUID STUDENT_2_UUID = UUID.fromString("11111111-1111-1111-1111-000000000102");
    public static final UUID STUDENT_3_UUID = UUID.fromString("11111111-1111-1111-1111-000000000103");
    public static final UUID STUDENT_4_UUID = UUID.fromString("11111111-1111-1111-1111-000000000104");
    public static final UUID INSTRUCTOR_UUID = UUID.fromString("22222222-2222-2222-2222-000000000007");

    @Override
    public void run(String... args) {
        if (reviewRepository.count() > 0) {
            return;
        }

        log.info("Initializing sample review and rating data for LearnHub LMS...");

        // Review 1 for Course 1
        Review r1 = Review.builder()
                .courseId(1)
                .studentId(STUDENT_1_UUID)
                .rating(5)
                .comment("Khóa học rất bài bản và chi tiết, dễ hiểu cho người mới bắt đầu microservices!")
                .status(ReviewStatus.APPROVED)
                .helpfulCount(12)
                .build();

        ReviewReply reply1 = ReviewReply.builder()
                .review(r1)
                .instructorId(INSTRUCTOR_UUID)
                .content("Cảm ơn bạn đã phản hồi tích cực! Chúc bạn hoàn thành tốt các dự án thực tế.")
                .build();
        r1.setReply(reply1);
        reviewRepository.save(r1);

        // Review 2 for Course 1
        Review r2 = Review.builder()
                .courseId(1)
                .studentId(STUDENT_2_UUID)
                .rating(4)
                .comment("Nội dung hay, bài giảng xúc tích. Mong giảng viên bổ sung thêm phần CI/CD.")
                .status(ReviewStatus.APPROVED)
                .helpfulCount(5)
                .build();
        reviewRepository.save(r2);

        // Review 3 for Course 1
        Review r3 = Review.builder()
                .courseId(1)
                .studentId(STUDENT_3_UUID)
                .rating(5)
                .comment("Tuyệt vời! Kiến thức Spring Boot và Eureka Gateway rất sát với thực tế doanh nghiệp.")
                .status(ReviewStatus.APPROVED)
                .helpfulCount(8)
                .build();
        reviewRepository.save(r3);

        // Recalculate summary for Course 1
        aggregationService.recalculateSummary(1);

        // Review 4 for Course 2
        Review r4 = Review.builder()
                .courseId(2)
                .studentId(STUDENT_4_UUID)
                .rating(5)
                .comment("Khóa học giải thích cặn kẽ về event-driven architecture và PostgreSQL database design.")
                .status(ReviewStatus.APPROVED)
                .helpfulCount(3)
                .build();
        reviewRepository.save(r4);

        aggregationService.recalculateSummary(2);

        log.info("Sample review & rating data initialized successfully with UUID user identifiers.");
    }
}
