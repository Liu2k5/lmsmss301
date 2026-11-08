
package com.example.progress_service.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import com.example.progress_service.entity.*;
import com.example.progress_service.event.StudentUnenrolledEvent;
import com.example.progress_service.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ArchiveProgressService {

    private final CourseProgressRepository progressRepository;

    private final LessonProgressRepository lessonRepository;

    private final QuizAttemptRepository quizRepository;

    private final ArchivedProgressRepository archiveRepository;

    private final ProgressEventRepository eventRepository;

    private final ObjectMapper objectMapper;

    @Transactional
    public void archiveProgress(StudentUnenrolledEvent event)
            throws JacksonException {

        // 1. Kiểm tra event đã xử lý chưa
        if (eventRepository.existsByEventId(event.getEventId())) {
            return;
        }

        // 2. Tìm tiến độ của học viên trong khóa học
        CourseProgress progress =
                progressRepository
                        .findByStudentIdAndCourseId(
                                event.getStudentId(),
                                event.getCourseId()
                        )
                        .orElse(null);

        // 3. Không có progress thì ghi nhận event
        if (progress == null) {
            saveProcessedEvent(event);
            return;
        }

        // 4. Lấy lesson progress
        List<LessonProgress> lessons =
                lessonRepository
                        .findByCourseProgressId(progress.getId());

        // 5. Lấy quiz attempts
        List<QuizAttempt> attempts =
                quizRepository
                        .findByCourseProgressId(progress.getId());

        // 6. Tạo snapshot
        Map<String, Object> snapshot = new LinkedHashMap<>();

        snapshot.put("courseProgress", createCourseSnapshot(progress));
        snapshot.put("lessonProgress", createLessonSnapshots(lessons));
        snapshot.put("quizAttempts", createQuizSnapshots(attempts));

        // 7. Chuyển snapshot thành JSON
        String json = objectMapper.writeValueAsString(snapshot);

        // 8. Tạo bản archive
        ArchivedProgress archived =
                ArchivedProgress.builder()
                        .studentId(progress.getStudentId())
                        .courseId(progress.getCourseId())
                        .completionPercentage(
                                progress.getCompletionPercentage()
                        )
                        .archivedData(json)
                        .archivedAt(LocalDateTime.now())
                        .build();

        archiveRepository.save(archived);

        // 9. Xóa các LessonProgress trước
        lessonRepository.deleteAll(lessons);

        // 10. Xóa các QuizAttempt trước
        quizRepository.deleteAll(attempts);

        // 11. Xóa CourseProgress sau khi đã xóa các bản ghi con
        progressRepository.delete(progress);

        // 12. Ghi nhận event đã xử lý
        saveProcessedEvent(event);
    }

    // Snapshot CourseProgress
    private Map<String, Object> createCourseSnapshot(
            CourseProgress progress) {

        Map<String, Object> data = new LinkedHashMap<>();

        data.put("id", progress.getId());
        data.put("studentId", progress.getStudentId());
        data.put("courseId", progress.getCourseId());

        data.put("curriculumVersion", progress.getCurriculumVersion());

        data.put("completionPercentage",
                progress.getCompletionPercentage());

        data.put("completedLessons", progress.getCompletedLessons());
        data.put("totalLessons", progress.getTotalLessons());

        data.put("averageQuizScore", progress.getAverageQuizScore());

        data.put("status", progress.getStatus());

        data.put("lastLessonId", progress.getLastLessonId());
        data.put("lastWatchPosition", progress.getLastWatchPosition());

        data.put("enrolledAt", progress.getEnrolledAt());
        data.put("lastAccessedAt", progress.getLastAccessedAt());
        data.put("completedAt", progress.getCompletedAt());

        data.put("createdAt", progress.getCreatedAt());
        data.put("updatedAt", progress.getUpdatedAt());

        return data;
    }

    // Snapshot LessonProgress
    private List<Map<String, Object>> createLessonSnapshots(
            List<LessonProgress> lessons) {

        List<Map<String, Object>> result = new ArrayList<>();

        for (LessonProgress lesson : lessons) {

            Map<String, Object> data = new LinkedHashMap<>();

            data.put("id", lesson.getId());
            data.put("lessonId", lesson.getLessonId());

            data.put("completed", lesson.getCompleted());

            data.put("watchPosition", lesson.getWatchPosition());
            data.put("videoDuration", lesson.getVideoDuration());

            data.put("completedAt", lesson.getCompletedAt());
            data.put("lastWatchedAt", lesson.getLastWatchedAt());

            data.put("createdAt", lesson.getCreatedAt());
            data.put("updatedAt", lesson.getUpdatedAt());

            data.put(
                    "includedInCurriculum",
                    lesson.getIncludedInCurriculum()
            );

            result.add(data);
        }

        return result;
    }

    // Snapshot QuizAttempt
    private List<Map<String, Object>> createQuizSnapshots(
            List<QuizAttempt> attempts) {

        List<Map<String, Object>> result = new ArrayList<>();

        for (QuizAttempt attempt : attempts) {

            Map<String, Object> data = new LinkedHashMap<>();

            data.put("id", attempt.getId());
            data.put("quizId", attempt.getQuizId());

            data.put("attemptNumber", attempt.getAttemptNumber());

            data.put("score", attempt.getScore());
            data.put("passed", attempt.getPassed());

            data.put("submittedAt", attempt.getSubmittedAt());

            result.add(data);
        }

        return result;
    }

    // Ghi nhận event
    private void saveProcessedEvent(StudentUnenrolledEvent event) {

        ProgressEvent progressEvent =
                ProgressEvent.builder()
                        .eventId(event.getEventId())
                        .eventType("StudentUnenrolled")
                        .build();

        eventRepository.save(progressEvent);
    }
}
