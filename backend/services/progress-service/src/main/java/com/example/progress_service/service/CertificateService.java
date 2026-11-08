package com.example.progress_service.service;

import com.example.progress_service.entity.*;
import com.example.progress_service.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CertificateService {

    private final CourseProgressRepository progressRepository;

    private final CourseCertificateRepository certificateRepository;

    private final LessonProgressRepository lessonRepository;

    private final QuizAttemptRepository quizRepository;

    private final CourseServiceClient courseServiceClient;

    /**
     * Cấp chứng chỉ khi học viên hoàn thành khóa học.
     */
    @Transactional
    public CourseCertificate issueCertificate(UUID progressId) {

        // 1. Kiểm tra progressId
        if (progressId == null) {
            throw new IllegalArgumentException(
                    "Progress ID must be greater than 0");
        }

        // 2. Tìm tiến độ khóa học
        CourseProgress progress =
                progressRepository.findById(progressId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Course progress not found"));

        // 3. Kiểm tra trạng thái khóa học
        if (!"COMPLETED".equals(progress.getStatus())) {
            throw new IllegalStateException(
                    "Course is not completed");
        }

        // 4. Kiểm tra phần trăm hoàn thành
        if (progress.getCompletionPercentage() == null
                || progress.getCompletionPercentage()
                .compareTo(BigDecimal.valueOf(100)) < 0) {

            throw new IllegalStateException(
                    "Course completion percentage must be 100%");
        }

        // 5. Kiểm tra khóa học có bài học hay không
        if (progress.getTotalLessons() == null
                || progress.getTotalLessons() <= 0) {

            throw new IllegalStateException(
                    "Course has no lessons");
        }

        // 6. Kiểm tra tất cả bài học đã hoàn thành
        long incompleteLessons =
                lessonRepository
                        .countByCourseProgressIdAndCompleted(
                                progressId,
                                false);

        if (incompleteLessons > 0) {
            throw new IllegalStateException(
                    "Some lessons are not completed");
        }

        // 7. Kiểm tra chứng chỉ đã được cấp chưa
        // Nếu đã cấp thì trả về chứng chỉ hiện tại
        var existingCertificate =
                certificateRepository
                        .findByCourseProgressId(progressId);

        if (existingCertificate.isPresent()) {
            return existingCertificate.get();
        }

        // 8. Lấy danh sách quiz bắt buộc từ Course Service
        List<UUID> requiredQuizIds =
                courseServiceClient.getRequiredQuizIds(
                        progress.getCourseId());

        // 9. Kiểm tra dữ liệu quiz
        if (requiredQuizIds == null) {
            throw new IllegalStateException(
                    "Cannot retrieve required quizzes from Course Service");
        }

        // 10. Kiểm tra học viên đã pass tất cả quiz bắt buộc
        for (UUID quizId : requiredQuizIds) {

            if (quizId == null) {
                throw new IllegalStateException(
                        "Invalid required quiz ID");
            }

            var attempts =
                    quizRepository
                            .findByCourseProgressIdAndQuizId(
                                    progressId,
                                    quizId);

            boolean passed =
                    attempts.stream()
                            .anyMatch(attempt ->
                                    Boolean.TRUE.equals(
                                            attempt.getPassed()));

            if (!passed) {
                throw new IllegalStateException(
                        "Required quiz not passed: " + quizId);
            }
        }

        // 11. Tạo chứng chỉ
        CourseCertificate certificate =
                CourseCertificate.builder()
                        .courseProgress(progress)
                        .studentId(progress.getStudentId())
                        .courseId(progress.getCourseId())
                        .certificateCode(
                                UUID.randomUUID().toString())
                        .build();

        // 12. Lưu chứng chỉ
        return certificateRepository.save(certificate);
    }
}