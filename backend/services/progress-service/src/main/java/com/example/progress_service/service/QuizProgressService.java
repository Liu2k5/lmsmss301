package com.example.progress_service.service;

import com.example.progress_service.entity.CourseProgress;
import com.example.progress_service.entity.ProgressEvent;
import com.example.progress_service.entity.QuizAttempt;
import com.example.progress_service.event.QuizSubmittedEvent;
import com.example.progress_service.repository.CourseProgressRepository;
import com.example.progress_service.repository.ProgressEventRepository;
import com.example.progress_service.repository.QuizAttemptRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizProgressService {

    private final CourseProgressRepository progressRepository;
    private final QuizAttemptRepository quizRepository;
    private final ProgressEventRepository eventRepository;

    /**
     * UC-P04:
     * Nhận kết quả Quiz từ Course Service thông qua RabbitMQ.
     */
    @Transactional
    public void recordQuizResult(QuizSubmittedEvent event) {

        // 1. Kiểm tra event
        validateEvent(event);

        // 2. Chống xử lý event trùng
        if (eventRepository.existsByEventId(event.getEventId())) {
            System.out.println(
                    "Quiz event already processed: " + event.getEventId());
            return;
        }

        // 3. Tìm tiến độ khóa học của học viên
        CourseProgress progress = progressRepository
                .findByStudentIdAndCourseId(
                        event.getStudentId(),
                        event.getCourseId())
                .orElseThrow(() ->
                        new RuntimeException("Course progress not found"));

        // 4. Kiểm tra attempt đã tồn tại chưa
        boolean attemptExists = quizRepository
                .findByCourseProgressIdAndQuizId(
                        progress.getId(),
                        event.getQuizId())
                .stream()
                .anyMatch(attempt ->
                        attempt.getAttemptNumber()
                                .equals(event.getAttemptNumber()));

        if (attemptExists) {
            throw new RuntimeException(
                    "Quiz attempt number already exists");
        }

        // 5. Xác định kết quả đạt hay trượt
        boolean passed = event.getScore()
                .compareTo(event.getPassingScore()) >= 0;

        // 6. Tạo bản ghi QuizAttempt
        QuizAttempt attempt = QuizAttempt.builder()
                .courseProgress(progress)
                .quizId(event.getQuizId())
                .attemptNumber(event.getAttemptNumber())
                .score(event.getScore())
                .passed(passed)
                .build();

        quizRepository.save(attempt);

        // 7. Cập nhật điểm Quiz trung bình
        updateAverageQuizScore(progress);

        // 8. Lưu event đã xử lý
        ProgressEvent processedEvent = ProgressEvent.builder()
                .eventId(event.getEventId())
                .eventType("QuizSubmitted")
                .build();

        eventRepository.save(processedEvent);

        System.out.println(
                "Quiz result recorded successfully. Event ID: "
                        + event.getEventId());
    }

    /**
     * Kiểm tra dữ liệu event trước khi lưu.
     */
    private void validateEvent(QuizSubmittedEvent event) {

        if (event == null) {
            throw new IllegalArgumentException(
                    "Quiz event cannot be null");
        }

        if (event.getEventId() == null
                || event.getEventId().isBlank()) {
            throw new IllegalArgumentException(
                    "Event ID is required");
        }

        if (event.getStudentId() == null
                || event.getCourseId() == null
                || event.getQuizId() == null) {
            throw new IllegalArgumentException(
                    "Student ID, Course ID and Quiz ID are required");
        }

        if (event.getAttemptNumber() == null
                || event.getAttemptNumber() <= 0) {
            throw new IllegalArgumentException(
                    "Attempt number must be greater than zero");
        }

        if (event.getScore() == null
                || event.getPassingScore() == null) {
            throw new IllegalArgumentException(
                    "Score and passing score are required");
        }

        BigDecimal minScore = BigDecimal.ZERO;
        BigDecimal maxScore = new BigDecimal("100");

        if (event.getScore().compareTo(minScore) < 0
                || event.getScore().compareTo(maxScore) > 0) {
            throw new IllegalArgumentException(
                    "Score must be between 0 and 100");
        }

        if (event.getPassingScore().compareTo(minScore) < 0
                || event.getPassingScore().compareTo(maxScore) > 0) {
            throw new IllegalArgumentException(
                    "Passing score must be between 0 and 100");
        }
    }

    /**
     * Tính điểm Quiz trung bình của học viên trong khóa học.
     */
    private void updateAverageQuizScore(CourseProgress progress) {

        List<QuizAttempt> attempts =
                quizRepository.findByCourseProgressId(
                        progress.getId());

        if (attempts.isEmpty()) {
            progress.setAverageQuizScore(BigDecimal.ZERO);
            progressRepository.save(progress);
            return;
        }

        BigDecimal total = attempts.stream()
                .map(QuizAttempt::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal average = total.divide(
                BigDecimal.valueOf(attempts.size()),
                2,
                RoundingMode.HALF_UP);

        progress.setAverageQuizScore(average);

        progressRepository.save(progress);
    }
}