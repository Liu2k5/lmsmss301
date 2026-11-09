package com.example.progress_service.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.progress_service.client.EnrollmentClient;
import com.example.progress_service.dto.QuestionKey;
import com.example.progress_service.dto.QuizAnswerKey;
import com.example.progress_service.dto.QuizAttemptResponse;
import com.example.progress_service.dto.QuizSubmitResult;
import com.example.progress_service.dto.SubmitQuizRequest;
import com.example.progress_service.entity.CourseProgress;
import com.example.progress_service.entity.QuizAttempt;
import com.example.progress_service.event.QuizSubmittedEvent;
import com.example.progress_service.event.StudentEnrolledEvent;
import com.example.progress_service.repository.CourseProgressRepository;
import com.example.progress_service.repository.LessonProgressRepository;
import com.example.progress_service.repository.QuizAttemptRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * UC-P04 — Nộp bài quiz, chấm điểm, lưu lịch sử các lần làm.
 *
 * <p>Chấm điểm tại Progress Service (service sở hữu tiến độ): lấy đề + đáp án đúng từ
 * Course Service qua API nội bộ, so khớp đáp án học viên chọn, lưu {@link QuizAttempt},
 * cập nhật điểm quiz trung bình và (nếu đạt) đánh dấu bài học quiz đã hoàn thành.
 * Giới hạn số lần làm lấy từ cấu hình quiz ({@code maxAttempts}, null = không giới hạn).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuizSubmissionService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final CourseServiceClient courseServiceClient;

    private final EnrollmentClient enrollmentClient;

    private final ProgressService progressService;

    private final CourseProgressRepository progressRepository;

    private final QuizAttemptRepository quizAttemptRepository;

    private final LessonProgressRepository lessonProgressRepository;

    private final QuizProgressService quizProgressService;

    private final LessonProgressService lessonProgressService;

    @Transactional
    public QuizSubmitResult submit(UUID courseId, UUID quizId, SubmitQuizRequest request) {

        if (courseId == null || quizId == null || request == null || request.studentId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Course ID, quiz ID and student ID are required");
        }

        QuizAnswerKey key = courseServiceClient.fetchAnswerKey(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Không lấy được đề quiz từ Course Service"));

        // Quiz phải thuộc đúng khoá học trên đường dẫn.
        if (!courseId.equals(key.courseId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Quiz không thuộc khoá học này");
        }

        CourseProgress progress = progressRepository
                .findByStudentIdAndCourseId(request.studentId(), courseId)
                .orElseGet(() -> ensureProgress(request.studentId(), courseId));

        int attemptsDone = quizAttemptRepository
                .countByCourseProgressIdAndQuizId(progress.getId(), quizId);
        if (key.maxAttempts() != null && attemptsDone >= key.maxAttempts()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Bạn đã dùng hết " + key.maxAttempts() + " lần làm quiz này");
        }

        int correctCount = countCorrectAnswers(key, request.answers());
        int totalQuestions = key.questions().size();
        BigDecimal score = totalQuestions == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(correctCount)
                        .multiply(HUNDRED)
                        .divide(BigDecimal.valueOf(totalQuestions), 2, RoundingMode.HALF_UP);

        int attemptNumber = attemptsDone + 1;
        boolean passed = score.compareTo(key.passingScore()) >= 0;

        // Tái sử dụng luồng ghi nhận kết quả (lưu attempt, cập nhật điểm trung bình, chống trùng event).
        QuizSubmittedEvent event = new QuizSubmittedEvent(
                UUID.randomUUID().toString(),
                request.studentId(),
                courseId,
                quizId,
                score,
                key.passingScore(),
                attemptNumber);
        quizProgressService.recordQuizResult(event);

        boolean lessonCompleted = passed && completeQuizLesson(request.studentId(), courseId, key.lessonId());

        Integer maxAttempts = key.maxAttempts();
        int remaining = maxAttempts == null ? -1 : Math.max(0, maxAttempts - attemptNumber);

        return new QuizSubmitResult(quizId, courseId, attemptNumber, maxAttempts, score,
                key.passingScore(), passed, correctCount, totalQuestions, lessonCompleted, remaining);
    }

    /**
     * Học viên đã đăng ký nhưng Progress Service chưa có tiến độ (event StudentEnrolled bị lỡ,
     * hoặc đăng ký từ trước khi có event): kiểm tra quyền với Enrollment Service rồi khởi tạo lại.
     */
    private CourseProgress ensureProgress(UUID studentId, UUID courseId) {

        boolean allowed = enrollmentClient.check(studentId, courseId)
                .map(status -> status.enrolled() && status.accessGranted())
                .orElse(false);

        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Bạn cần đăng ký khoá học trước khi làm quiz");
        }

        List<UUID> lessonIds = courseServiceClient.fetchPublishedLessonIds(courseId);
        if (lessonIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Khoá học chưa có bài học để theo dõi tiến độ");
        }

        progressService.initializeProgress(new StudentEnrolledEvent(
                UUID.randomUUID().toString(), studentId, courseId, lessonIds));

        return progressRepository.findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Không khởi tạo được tiến độ khoá học"));
    }

    /** Lịch sử các lần làm quiz của học viên, mới nhất trước. */
    @Transactional(readOnly = true)
    public List<QuizAttemptResponse> attempts(UUID studentId, UUID courseId, UUID quizId) {

        CourseProgress progress = progressRepository
                .findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Course progress not found"));

        return quizAttemptRepository.findByCourseProgressIdAndQuizId(progress.getId(), quizId).stream()
                .sorted((a, b) -> Integer.compare(b.getAttemptNumber(), a.getAttemptNumber()))
                .map(attempt -> new QuizAttemptResponse(
                        attempt.getId(),
                        attempt.getQuizId(),
                        attempt.getAttemptNumber(),
                        attempt.getScore(),
                        Boolean.TRUE.equals(attempt.getPassed()),
                        attempt.getSubmittedAt()))
                .toList();
    }

    /**
     * So khớp đáp án: câu đúng khi tập đáp án chọn bằng đúng tập đáp án đúng
     * (MULTIPLE_CHOICE phải chọn đủ và không thừa). Câu không nộp = sai.
     */
    private int countCorrectAnswers(QuizAnswerKey key, List<SubmitQuizRequest.SubmittedAnswer> answers) {

        Map<UUID, Set<UUID>> correctByQuestion = new HashMap<>();
        for (QuestionKey question : key.questions()) {
            correctByQuestion.put(question.questionId(),
                    question.correctOptionIds() == null ? Set.of() : new HashSet<>(question.correctOptionIds()));
        }

        Map<UUID, Set<UUID>> submittedByQuestion = new HashMap<>();
        for (SubmitQuizRequest.SubmittedAnswer answer : answers) {
            UUID questionId = answer.questionId();
            if (questionId == null || !correctByQuestion.containsKey(questionId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Câu hỏi không thuộc quiz này: " + questionId);
            }
            submittedByQuestion.put(questionId,
                    answer.optionIds() == null ? Set.of() : new HashSet<>(answer.optionIds()));
        }

        int correct = 0;
        for (Map.Entry<UUID, Set<UUID>> entry : correctByQuestion.entrySet()) {
            Set<UUID> chosen = submittedByQuestion.getOrDefault(entry.getKey(), Set.of());
            if (!chosen.isEmpty() && chosen.equals(entry.getValue())) {
                correct++;
            }
        }
        return correct;
    }

    /** Đạt quiz thì đánh dấu bài học quiz hoàn thành; bài học không được theo dõi thì bỏ qua. */
    private boolean completeQuizLesson(UUID studentId, UUID courseId, UUID lessonId) {

        if (lessonId == null) {
            return false;
        }
        CourseProgress progress = progressRepository
                .findByStudentIdAndCourseId(studentId, courseId)
                .orElse(null);
        if (progress == null || lessonProgressRepository
                .findByCourseProgressIdAndLessonId(progress.getId(), lessonId).isEmpty()) {
            return false;
        }
        try {
            lessonProgressService.markLessonCompleted(studentId, courseId, lessonId);
            return true;
        }
        catch (RuntimeException ex) {
            log.warn("Không đánh dấu được bài học {} hoàn thành: {}", lessonId, ex.getMessage());
            return false;
        }
    }
}
