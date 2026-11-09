package com.example.progress_service.service;

import java.util.UUID;
import com.example.progress_service.entity.CourseProgress;
import com.example.progress_service.entity.LessonProgress;
import com.example.progress_service.repository.CourseProgressRepository;
import com.example.progress_service.repository.LessonProgressRepository;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LessonProgressService {

    private final CourseProgressRepository progressRepository;

    private final LessonProgressRepository lessonRepository;

    private final ProgressService progressService;

    private final CourseServiceClient courseServiceClient;

    /**
     * Bài học được thêm vào curriculum sau khi học viên đã đăng ký nên chưa có dòng tiến độ:
     * tạo mới nếu bài học thuộc curriculum đã xuất bản của khoá (UC-P10 một phần), ngược lại báo 404.
     */
    private LessonProgress trackNewLesson(CourseProgress progress, UUID courseId, UUID lessonId) {

        List<UUID> publishedLessonIds = courseServiceClient.fetchPublishedLessonIds(courseId);
        if (publishedLessonIds == null || !publishedLessonIds.contains(lessonId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Bài học không thuộc khoá học đã xuất bản nên không theo dõi được tiến độ");
        }

        LessonProgress created = lessonRepository.save(LessonProgress.builder()
                .courseProgress(progress)
                .lessonId(lessonId)
                .completed(false)
                .watchPosition(0)
                .videoDuration(0)
                .includedInCurriculum(true)
                .build());

        progress.setTotalLessons((progress.getTotalLessons() == null ? 0 : progress.getTotalLessons()) + 1);
        progressRepository.save(progress);
        return created;
    }

    /**
     * UC-P02: Đánh dấu lesson hoàn thành thủ công.
     */
    @Transactional
    public void markLessonCompleted(
            UUID studentId,
            UUID courseId,
            UUID lessonId) {

        // Kiểm tra dữ liệu đầu vào
        if (studentId == null || courseId == null || lessonId == null) {
            throw new IllegalArgumentException(
                    "Student ID, Course ID and Lesson ID are required");
        }

        // Tìm tiến độ khóa học của học viên
        CourseProgress progress = progressRepository
                .findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Course progress not found"));

        // Tìm lesson trong tiến độ khóa học
        LessonProgress lesson = lessonRepository
                .findByCourseProgressIdAndLessonId(
                        progress.getId(),
                        lessonId)
                .orElseGet(() -> trackNewLesson(progress, courseId, lessonId));

        // Chỉ cập nhật nếu lesson chưa hoàn thành
        if (!Boolean.TRUE.equals(lesson.getCompleted())) {

            lesson.setCompleted(true);

            lesson.setCompletedAt(LocalDateTime.now());

            // Đánh dấu video đã xem hết nếu có duration
            if (lesson.getVideoDuration() != null
                    && lesson.getVideoDuration() > 0) {

                lesson.setWatchPosition(lesson.getVideoDuration());
            }

            lessonRepository.save(lesson);
        }

        // Tính lại tiến độ khóa học
        progressService.recalculateProgress(progress.getId());
    }

    @Transactional
    public void saveWatchPosition(
            UUID studentId,
            UUID courseId,
            UUID lessonId,
            Integer watchPosition,
            Integer videoDuration) {

        // 1. Kiểm tra dữ liệu đầu vào
        if (studentId == null || courseId == null || lessonId == null) {
            throw new IllegalArgumentException(
                    "Student ID, Course ID and Lesson ID are required");
        }

        if (watchPosition == null || videoDuration == null
                || watchPosition < 0 || videoDuration <= 0) {
            throw new IllegalArgumentException(
                    "Invalid watch position or video duration");
        }

        if (watchPosition > videoDuration) {
            throw new IllegalArgumentException(
                    "Watch position cannot exceed video duration");
        }

        // 2. Tìm tiến độ khóa học
        CourseProgress progress = progressRepository
                .findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Course progress not found"));

        // 3. Tìm tiến độ bài học
        LessonProgress lesson = lessonRepository
                .findByCourseProgressIdAndLessonId(
                        progress.getId(), lessonId)
                .orElseThrow(() ->
                        new RuntimeException("Lesson progress not found"));

        // 4. Lưu vị trí video
        lesson.setWatchPosition(watchPosition);
        lesson.setVideoDuration(videoDuration);
        lesson.setLastWatchedAt(LocalDateTime.now());

        // 5. Tự động hoàn thành khi xem đạt >= 90%
        double watchedPercentage =
                (double) watchPosition / videoDuration;

        if (watchedPercentage >= 0.90
                && !Boolean.TRUE.equals(lesson.getCompleted())) {

            lesson.setCompleted(true);
            lesson.setCompletedAt(LocalDateTime.now());

            // Đánh dấu video đã xem hết
            lesson.setWatchPosition(videoDuration);
        }

        lessonRepository.save(lesson);

        // 6. Cập nhật bài học gần nhất trong CourseProgress
        progress.setLastLessonId(lessonId);

        progress.setLastWatchPosition(
                lesson.getWatchPosition());

        progress.setLastAccessedAt(LocalDateTime.now());

        progressRepository.save(progress);

        // 7. Tính lại tiến độ khóa học
        progressService.recalculateProgress(progress.getId());
    }
}