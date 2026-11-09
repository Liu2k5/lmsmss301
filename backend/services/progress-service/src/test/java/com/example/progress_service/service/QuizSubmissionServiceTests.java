package com.example.progress_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.example.progress_service.client.EnrollmentClient;
import com.example.progress_service.dto.EnrollmentStatus;
import com.example.progress_service.dto.QuestionKey;
import com.example.progress_service.dto.QuizAnswerKey;
import com.example.progress_service.dto.SubmitQuizRequest;
import com.example.progress_service.entity.CourseProgress;
import com.example.progress_service.entity.LessonProgress;
import com.example.progress_service.entity.QuizAttempt;
import com.example.progress_service.event.QuizSubmittedEvent;
import com.example.progress_service.event.StudentEnrolledEvent;
import com.example.progress_service.repository.CourseProgressRepository;
import com.example.progress_service.repository.LessonProgressRepository;
import com.example.progress_service.repository.QuizAttemptRepository;

/** UC-P04: chấm điểm, giới hạn số lần làm và lưu lịch sử. */
@ExtendWith(MockitoExtension.class)
class QuizSubmissionServiceTests {

    private static final UUID STUDENT = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID COURSE = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID QUIZ = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID LESSON = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID Q1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID Q2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID Q1_RIGHT = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID Q1_WRONG = UUID.fromString("00000000-0000-0000-0000-000000000012");
    private static final UUID Q2_RIGHT_A = UUID.fromString("00000000-0000-0000-0000-000000000021");
    private static final UUID Q2_RIGHT_B = UUID.fromString("00000000-0000-0000-0000-000000000022");
    private static final UUID Q2_WRONG = UUID.fromString("00000000-0000-0000-0000-000000000023");

    @Mock
    private CourseServiceClient courseServiceClient;

    @Mock
    private EnrollmentClient enrollmentClient;

    @Mock
    private ProgressService progressService;

    @Mock
    private CourseProgressRepository progressRepository;

    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    @Mock
    private LessonProgressRepository lessonProgressRepository;

    @Mock
    private QuizProgressService quizProgressService;

    @Mock
    private LessonProgressService lessonProgressService;

    private QuizSubmissionService service;

    private CourseProgress progress;

    @BeforeEach
    void setUp() {
        service = new QuizSubmissionService(courseServiceClient, enrollmentClient, progressService,
                progressRepository, quizAttemptRepository, lessonProgressRepository, quizProgressService,
                lessonProgressService);

        progress = new CourseProgress();
        progress.setId(UUID.randomUUID());
        progress.setStudentId(STUDENT);
        progress.setCourseId(COURSE);
    }

    @Test
    @DisplayName("Chấm đúng 1/2 câu -> 50%, không đạt, lưu lượt 1")
    void chamDiemMotNuaSoCau() {
        when(courseServiceClient.fetchAnswerKey(QUIZ)).thenReturn(Optional.of(answerKey(3)));
        when(progressRepository.findByStudentIdAndCourseId(STUDENT, COURSE)).thenReturn(Optional.of(progress));
        when(quizAttemptRepository.countByCourseProgressIdAndQuizId(progress.getId(), QUIZ)).thenReturn(0);

        var result = service.submit(COURSE, QUIZ, new SubmitQuizRequest(STUDENT, List.of(
                new SubmitQuizRequest.SubmittedAnswer(Q1, List.of(Q1_RIGHT)),
                new SubmitQuizRequest.SubmittedAnswer(Q2, List.of(Q2_WRONG)))));

        assertEquals(1, result.attemptNumber());
        assertEquals(1, result.correctCount());
        assertEquals(2, result.totalQuestions());
        assertEquals(new BigDecimal("50.00"), result.score());
        assertFalse(result.passed());
        assertEquals(2, result.remainingAttempts());
        assertFalse(result.lessonCompleted());

        ArgumentCaptor<QuizSubmittedEvent> event = ArgumentCaptor.forClass(QuizSubmittedEvent.class);
        verify(quizProgressService).recordQuizResult(event.capture());
        assertEquals(new BigDecimal("50.00"), event.getValue().getScore());
        assertEquals(1, event.getValue().getAttemptNumber());
        assertEquals(STUDENT, event.getValue().getStudentId());
    }

    @Test
    @DisplayName("Trả lời đủ và đúng hết -> 100%, đạt, đánh dấu bài học hoàn thành")
    void datQuizThiHoanThanhBaiHoc() {
        when(courseServiceClient.fetchAnswerKey(QUIZ)).thenReturn(Optional.of(answerKey(null)));
        when(progressRepository.findByStudentIdAndCourseId(STUDENT, COURSE)).thenReturn(Optional.of(progress));
        when(quizAttemptRepository.countByCourseProgressIdAndQuizId(progress.getId(), QUIZ)).thenReturn(1);
        LessonProgress lesson = new LessonProgress();
        when(lessonProgressRepository.findByCourseProgressIdAndLessonId(progress.getId(), LESSON))
                .thenReturn(Optional.of(lesson));

        var result = service.submit(COURSE, QUIZ, new SubmitQuizRequest(STUDENT, List.of(
                new SubmitQuizRequest.SubmittedAnswer(Q1, List.of(Q1_RIGHT)),
                new SubmitQuizRequest.SubmittedAnswer(Q2, List.of(Q2_RIGHT_A, Q2_RIGHT_B)))));

        assertEquals(new BigDecimal("100.00"), result.score());
        assertTrue(result.passed());
        assertEquals(2, result.attemptNumber());
        assertTrue(result.lessonCompleted());
        assertEquals(-1, result.remainingAttempts(), "maxAttempts null = không giới hạn");
        verify(lessonProgressService).markLessonCompleted(STUDENT, COURSE, LESSON);
    }

    @Test
    @DisplayName("Chọn thừa đáp án ở câu nhiều lựa chọn -> câu đó sai")
    void chonThuaDapAnThiSai() {
        when(courseServiceClient.fetchAnswerKey(QUIZ)).thenReturn(Optional.of(answerKey(5)));
        when(progressRepository.findByStudentIdAndCourseId(STUDENT, COURSE)).thenReturn(Optional.of(progress));
        when(quizAttemptRepository.countByCourseProgressIdAndQuizId(progress.getId(), QUIZ)).thenReturn(0);

        var result = service.submit(COURSE, QUIZ, new SubmitQuizRequest(STUDENT, List.of(
                new SubmitQuizRequest.SubmittedAnswer(Q1, List.of(Q1_RIGHT)),
                new SubmitQuizRequest.SubmittedAnswer(Q2, List.of(Q2_RIGHT_A, Q2_RIGHT_B, Q2_WRONG)))));

        assertEquals(1, result.correctCount());
        assertEquals(new BigDecimal("50.00"), result.score());
    }

    @Test
    @DisplayName("Hết số lần làm -> 409, không ghi nhận lượt mới")
    void hetLuotLamBai() {
        when(courseServiceClient.fetchAnswerKey(QUIZ)).thenReturn(Optional.of(answerKey(2)));
        when(progressRepository.findByStudentIdAndCourseId(STUDENT, COURSE)).thenReturn(Optional.of(progress));
        when(quizAttemptRepository.countByCourseProgressIdAndQuizId(progress.getId(), QUIZ)).thenReturn(2);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.submit(COURSE, QUIZ, new SubmitQuizRequest(STUDENT, List.of())));

        assertEquals(409, ex.getStatusCode().value());
        verify(quizProgressService, never()).recordQuizResult(any());
    }

    @Test
    @DisplayName("Chưa đăng ký khoá học -> 403")
    void chuaDangKyKhoaHoc() {
        when(courseServiceClient.fetchAnswerKey(QUIZ)).thenReturn(Optional.of(answerKey(3)));
        when(progressRepository.findByStudentIdAndCourseId(STUDENT, COURSE)).thenReturn(Optional.empty());
        when(enrollmentClient.check(STUDENT, COURSE))
                .thenReturn(Optional.of(new EnrollmentStatus(false, "NONE", false)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.submit(COURSE, QUIZ, new SubmitQuizRequest(STUDENT, List.of())));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("Đã đăng ký nhưng thiếu bản ghi tiến độ -> tự khởi tạo rồi chấm điểm")
    void daDangKyThieuTienDoThiKhoiTaoLai() {
        when(courseServiceClient.fetchAnswerKey(QUIZ)).thenReturn(Optional.of(answerKey(3)));
        when(progressRepository.findByStudentIdAndCourseId(STUDENT, COURSE))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(progress));
        when(enrollmentClient.check(STUDENT, COURSE))
                .thenReturn(Optional.of(new EnrollmentStatus(true, "ACTIVE", true)));
        when(courseServiceClient.fetchPublishedLessonIds(COURSE)).thenReturn(List.of(LESSON));
        when(quizAttemptRepository.countByCourseProgressIdAndQuizId(progress.getId(), QUIZ)).thenReturn(0);
        when(lessonProgressRepository.findByCourseProgressIdAndLessonId(progress.getId(), LESSON))
                .thenReturn(Optional.empty());

        var result = service.submit(COURSE, QUIZ, new SubmitQuizRequest(STUDENT, List.of(
                new SubmitQuizRequest.SubmittedAnswer(Q1, List.of(Q1_RIGHT)),
                new SubmitQuizRequest.SubmittedAnswer(Q2, List.of(Q2_RIGHT_A, Q2_RIGHT_B)))));

        verify(progressService).initializeProgress(any(StudentEnrolledEvent.class));
        assertEquals(new BigDecimal("100.00"), result.score());
        assertTrue(result.passed());
    }

    @Test
    @DisplayName("Câu hỏi không thuộc quiz -> 400")
    void cauHoiKhongThuocQuiz() {
        when(courseServiceClient.fetchAnswerKey(QUIZ)).thenReturn(Optional.of(answerKey(3)));
        when(progressRepository.findByStudentIdAndCourseId(STUDENT, COURSE)).thenReturn(Optional.of(progress));
        when(quizAttemptRepository.countByCourseProgressIdAndQuizId(progress.getId(), QUIZ)).thenReturn(0);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.submit(COURSE, QUIZ, new SubmitQuizRequest(STUDENT, List.of(
                        new SubmitQuizRequest.SubmittedAnswer(UUID.randomUUID(), List.of(Q1_RIGHT))))));

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("Course Service không trả đề -> 503")
    void courseServiceKhongSanSang() {
        when(courseServiceClient.fetchAnswerKey(QUIZ)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.submit(COURSE, QUIZ, new SubmitQuizRequest(STUDENT, List.of())));

        assertEquals(503, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("Lịch sử làm bài trả về mới nhất trước")
    void lichSuLamBai() {
        when(progressRepository.findByStudentIdAndCourseId(STUDENT, COURSE)).thenReturn(Optional.of(progress));
        when(quizAttemptRepository.findByCourseProgressIdAndQuizId(progress.getId(), QUIZ))
                .thenReturn(List.of(attempt(1, "40.00"), attempt(2, "80.00")));

        var attempts = service.attempts(STUDENT, COURSE, QUIZ);

        assertEquals(2, attempts.size());
        assertEquals(2, attempts.get(0).attemptNumber());
        assertEquals(new BigDecimal("80.00"), attempts.get(0).score());
    }

    private QuizAttempt attempt(int number, String score) {
        QuizAttempt attempt = new QuizAttempt();
        attempt.setId(UUID.randomUUID());
        attempt.setQuizId(QUIZ);
        attempt.setAttemptNumber(number);
        attempt.setScore(new BigDecimal(score));
        attempt.setPassed(false);
        attempt.setSubmittedAt(LocalDateTime.now());
        return attempt;
    }

    /** 2 câu: Q1 một đáp án đúng, Q2 nhiều đáp án đúng. */
    private QuizAnswerKey answerKey(Integer maxAttempts) {
        return new QuizAnswerKey(QUIZ, COURSE, LESSON, "Kiểm tra", new BigDecimal("70.00"), maxAttempts,
                List.of(
                        new QuestionKey(Q1, "SINGLE_CHOICE", List.of(Q1_RIGHT)),
                        new QuestionKey(Q2, "MULTIPLE_CHOICE", List.of(Q2_RIGHT_A, Q2_RIGHT_B))));
    }
}
