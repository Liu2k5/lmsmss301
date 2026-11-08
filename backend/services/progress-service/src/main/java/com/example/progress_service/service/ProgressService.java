package com.example.progress_service.service;

import com.example.progress_service.dto.CourseProgressResponse;
import com.example.progress_service.dto.LessonProgressResponse;
import com.example.progress_service.dto.ResumeLearningResponse;
import com.example.progress_service.entity.*;
import com.example.progress_service.event.StudentEnrolledEvent;
import com.example.progress_service.repository.*;


import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final CourseProgressRepository progressRepository;

    private final LessonProgressRepository lessonRepository;

    private final ProgressEventRepository eventRepository;

    @Transactional
    public void initializeProgress(StudentEnrolledEvent event) {

        // Kiểm tra event
        if (event == null) {
            throw new IllegalArgumentException("Event must not be null");
        }

        if (event.getEventId() == null || event.getEventId().isBlank()) {
            throw new IllegalArgumentException("Event ID must not be empty");
        }

        if (event.getStudentId() == null || event.getCourseId() == null) {
            throw new IllegalArgumentException("Student ID and Course ID are required");
        }

        if (event.getLessonIds() == null || event.getLessonIds().isEmpty()) {
            throw new IllegalArgumentException("Lesson IDs must not be empty");
        }

        // Kiểm tra event đã xử lý chưa
        if (eventRepository.existsByEventId(event.getEventId())) {
            System.out.println("Event already processed: " + event.getEventId());
            return;
        }

        // Tạo CourseProgress
        CourseProgress progress = CourseProgress.builder()
                .studentId(event.getStudentId())
                .courseId(event.getCourseId())
                .completionPercentage(BigDecimal.ZERO)
                .completedLessons(0)
                .totalLessons(event.getLessonIds().size())
                .averageQuizScore(BigDecimal.ZERO)
                .status("IN_PROGRESS")
                .enrolledAt(LocalDateTime.now())
                .build();

        progress = progressRepository.save(progress);

        // Tạo progress cho từng lesson
        for (Integer lessonId : event.getLessonIds()) {

            LessonProgress lesson = LessonProgress.builder()
                    .courseProgress(progress)
                    .lessonId(lessonId)
                    .completed(false)
                    .watchPosition(0)
                    .videoDuration(0)
                    .build();

            lessonRepository.save(lesson);
        }

        saveEvent(event.getEventId(), "StudentEnrolled");
    }

    private void saveEvent(String eventId, String eventType) {

        ProgressEvent event = ProgressEvent.builder()
                .eventId(eventId)
                .eventType(eventType)
                .build();

        eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public CourseProgressResponse getCourseProgress(
            Integer studentId,
            Integer courseId) {

        CourseProgress progress =
                progressRepository
                        .findByStudentIdAndCourseId(
                                studentId,
                                courseId
                        )
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Course progress not found"));

        var lessons = lessonRepository
                .findByCourseProgressId(progress.getId());

        var lessonResponses = lessons.stream()
                .map(lesson -> LessonProgressResponse.builder()
                        .lessonId(lesson.getLessonId())
                        .completed(lesson.getCompleted())
                        .watchPosition(lesson.getWatchPosition())
                        .videoDuration(lesson.getVideoDuration())
                        .build())
                .toList();

        return CourseProgressResponse.builder()
                .studentId(studentId)
                .courseId(courseId)
                .completionPercentage(
                        progress.getCompletionPercentage()
                )
                .completedLessons(progress.getCompletedLessons())
                .totalLessons(progress.getTotalLessons())
                .averageQuizScore(progress.getAverageQuizScore())
                .status(progress.getStatus())
                .lessons(lessonResponses)
                .lastAccessedAt(progress.getLastAccessedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public ResumeLearningResponse resumeLearning(
            Integer studentId,
            Integer courseId) {

        // 1. Tìm tiến độ khóa học
        CourseProgress progress = progressRepository
                .findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Course progress not found"));

        // 2. Tìm bài học gần nhất đã được lưu
        LessonProgress lastLesson = null;

        if (progress.getLastLessonId() != null) {

            lastLesson = lessonRepository
                    .findByCourseProgressIdAndLessonId(
                            progress.getId(),
                            progress.getLastLessonId())
                    .orElse(null);
        }

        LessonProgress lessonToResume = null;

        // 3. Nếu bài gần nhất chưa hoàn thành,
        // tiếp tục bài đó
        if (lastLesson != null
                && Boolean.TRUE.equals(lastLesson.getIncludedInCurriculum())
                && !Boolean.TRUE.equals(lastLesson.getCompleted())) {

            lessonToResume = lastLesson;

        } else {

            // 4. Nếu bài gần nhất đã hoàn thành,
            // tìm bài chưa hoàn thành tiếp theo
            lessonToResume = lessonRepository
                    .findFirstByCourseProgressIdAndCompletedAndIncludedInCurriculumOrderByLessonIdAsc(
                            progress.getId(),
                            false,
                            true
                    )
                    .orElse(null);
        }

        // 5. Nếu chưa có bài học nào được lưu,
        // tìm bài chưa hoàn thành đầu tiên
        if (lessonToResume == null
                && lastLesson == null) {

            lessonToResume = lessonRepository
                    .findFirstByCourseProgressIdAndCompletedAndIncludedInCurriculumOrderByLessonIdAsc(
                            progress.getId(),
                            false,
                            true
                    )
                    .orElse(null);
        }

        // 6. Nếu tất cả bài học đã hoàn thành
        if (lessonToResume == null) {

            return ResumeLearningResponse.builder()
                    .courseId(courseId)
                    .lessonId(null)
                    .watchPosition(0)
                    .completed(true)
                    .build();
        }

        // 7. Trả về bài học cần tiếp tục
        return ResumeLearningResponse.builder()
                .courseId(courseId)
                .lessonId(lessonToResume.getLessonId())
                .watchPosition(
                        lessonToResume.getWatchPosition() != null
                                ? lessonToResume.getWatchPosition()
                                : 0)
                .completed(lessonToResume.getCompleted())
                .build();
    }

    @Transactional
    public void recalculateProgress(Integer progressId) {

        CourseProgress progress = progressRepository.findById(progressId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Course progress not found"));

        // Chỉ đếm bài học còn nằm trong curriculum
        long totalLessons =
                lessonRepository
                        .countByCourseProgressIdAndIncludedInCurriculum(
                                progressId,
                                true
                        );

        // Chỉ đếm bài học hoàn thành và còn trong curriculum
        long completedLessons =
                lessonRepository
                        .countByCourseProgressIdAndCompletedAndIncludedInCurriculum(
                                progressId,
                                true,
                                true
                        );

        // Cập nhật số lượng bài học
        progress.setCompletedLessons((int) completedLessons);
        progress.setTotalLessons((int) totalLessons);

        // Tính phần trăm tiến độ
        BigDecimal percentage = BigDecimal.ZERO;

        if (totalLessons > 0) {
            percentage = BigDecimal.valueOf(completedLessons)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(
                            BigDecimal.valueOf(totalLessons),
                            2,
                            RoundingMode.HALF_UP
                    );
        }

        progress.setCompletionPercentage(percentage);

        // Cập nhật trạng thái khóa học
        if (totalLessons > 0 && completedLessons == totalLessons) {

            progress.setStatus("COMPLETED");

            if (progress.getCompletedAt() == null) {
                progress.setCompletedAt(LocalDateTime.now());
            }

        } else {

            progress.setStatus("IN_PROGRESS");

            // Nếu curriculum thay đổi khiến khóa học chưa hoàn thành,
            // xóa thời điểm hoàn thành hiện tại
            progress.setCompletedAt(null);
        }

        progressRepository.save(progress);
    }
}