package com.example.progress_service.service;

import com.example.progress_service.dto.CourseAnalyticsResponse;
import com.example.progress_service.entity.CourseProgress;
import com.example.progress_service.repository.CourseProgressRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProgressAnalyticsService {

    private final CourseProgressRepository progressRepository;

    /**
     * Thống kê tiến độ học tập của một khóa học.
     */
    @Transactional(readOnly = true)
    public CourseAnalyticsResponse getCourseAnalytics(
            UUID courseId) {

        // 1. Kiểm tra courseId
        if (courseId == null) {
            throw new IllegalArgumentException(
                    "Course ID must be greater than 0");
        }

        // 2. Tổng số học viên đăng ký khóa học
        long totalStudents =
                progressRepository.countByCourseId(courseId);

        // 3. Số học viên đã hoàn thành khóa học
        long completedStudents =
                progressRepository
                        .countByCourseIdAndStatus(
                                courseId,
                                "COMPLETED");

        // 4. Tính tỷ lệ hoàn thành khóa học
        BigDecimal completionRate = BigDecimal.ZERO;

        if (totalStudents > 0) {

            completionRate =
                    BigDecimal.valueOf(completedStudents)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(
                                    BigDecimal.valueOf(totalStudents),
                                    2,
                                    RoundingMode.HALF_UP);
        }

        // 5. Lấy tiến độ của tất cả học viên trong khóa học
        List<CourseProgress> progresses =
                progressRepository.findByCourseId(courseId);

        // 6. Chỉ lấy học viên đã có điểm quiz
        List<BigDecimal> quizScores =
                progresses.stream()
                        .map(CourseProgress::getAverageQuizScore)
                        .filter(Objects::nonNull)
                        .toList();

        // 7. Tính điểm quiz trung bình
        BigDecimal averageQuizScore = BigDecimal.ZERO;

        if (!quizScores.isEmpty()) {

            BigDecimal totalScore =
                    quizScores.stream()
                            .reduce(
                                    BigDecimal.ZERO,
                                    BigDecimal::add);

            averageQuizScore =
                    totalScore.divide(
                            BigDecimal.valueOf(quizScores.size()),
                            2,
                            RoundingMode.HALF_UP);
        }

        // 8. Trả về kết quả thống kê
        return CourseAnalyticsResponse.builder()
                .courseId(courseId)
                .totalStudents((int) totalStudents)
                .completedStudents((int) completedStudents)
                .completionRate(completionRate)
                .averageQuizScore(averageQuizScore)
                .build();
    }
}