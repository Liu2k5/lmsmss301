package com.example.progress_service.service;

import com.example.progress_service.entity.CourseProgress;
import com.example.progress_service.entity.LessonProgress;
import com.example.progress_service.entity.ProgressEvent;
import com.example.progress_service.event.CurriculumChangedEvent;
import com.example.progress_service.repository.CourseProgressRepository;
import com.example.progress_service.repository.LessonProgressRepository;
import com.example.progress_service.repository.ProgressEventRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CurriculumUpdateService {

    private final CourseProgressRepository progressRepository;

    private final LessonProgressRepository lessonRepository;

    private final ProgressEventRepository eventRepository;

    private final ProgressService progressService;

    @Transactional
    public void handleCurriculumChanged(CurriculumChangedEvent event) {

        // 1. Kiểm tra event
        if (event == null) {
            throw new IllegalArgumentException("Event must not be null");
        }

        if (event.getEventId() == null
                || event.getEventId().isBlank()) {
            throw new IllegalArgumentException("Event ID is required");
        }

        if (event.getCourseId() == null
                || event.getCourseId() <= 0) {
            throw new IllegalArgumentException("Invalid course ID");
        }

        if (event.getCurriculumVersion() == null
                || event.getCurriculumVersion() <= 0) {
            throw new IllegalArgumentException(
                    "Curriculum version must be positive");
        }

        // 2. Kiểm tra event đã xử lý chưa
        if (eventRepository.existsByEventId(event.getEventId())) {
            return;
        }

        // 3. Lấy danh sách tiến độ của khóa học
        List<CourseProgress> progresses =
                progressRepository.findByCourseId(event.getCourseId());

        // 4. Cập nhật curriculum cho từng học viên
        for (CourseProgress progress : progresses) {

            Long currentVersion = progress.getCurriculumVersion();

            // Bỏ qua event có version cũ hoặc bằng version hiện tại
            if (currentVersion != null
                    && event.getCurriculumVersion() <= currentVersion) {
                continue;
            }

            // 5. Xử lý bài học được thêm
            addLessons(progress, event.getAddedLessonIds());

            // 6. Xử lý bài học bị loại
            removeLessons(progress, event.getRemovedLessonIds());

            // 7. Cập nhật curriculum version
            progress.setCurriculumVersion(
                    event.getCurriculumVersion()
            );

            progressRepository.save(progress);

            // 8. Tính lại tiến độ
            progressService.recalculateProgress(progress.getId());
        }

        // 9. Ghi nhận event đã xử lý
        ProgressEvent progressEvent = ProgressEvent.builder()
                .eventId(event.getEventId())
                .eventType("CurriculumChanged")
                .build();

        eventRepository.save(progressEvent);
    }

    private void addLessons(
            CourseProgress progress,
            List<Integer> addedLessonIds) {

        if (addedLessonIds == null) {
            return;
        }

        for (Integer lessonId : addedLessonIds) {

            if (lessonId == null || lessonId <= 0) {
                throw new IllegalArgumentException(
                        "Invalid lesson ID in addedLessonIds");
            }

            LessonProgress lesson =
                    lessonRepository
                            .findByCourseProgressIdAndLessonId(
                                    progress.getId(),
                                    lessonId
                            )
                            .orElse(null);

            if (lesson == null) {

                // Bài học hoàn toàn mới
                lesson = LessonProgress.builder()
                        .courseProgress(progress)
                        .lessonId(lessonId)
                        .completed(false)
                        .watchPosition(0)
                        .videoDuration(0)
                        .includedInCurriculum(true)
                        .build();

            } else {

                // Bài học từng bị loại nhưng nay được thêm lại
                lesson.setIncludedInCurriculum(true);
            }

            lessonRepository.save(lesson);
        }
    }

    private void removeLessons(
            CourseProgress progress,
            List<Integer> removedLessonIds) {

        if (removedLessonIds == null) {
            return;
        }

        for (Integer lessonId : removedLessonIds) {

            if (lessonId == null || lessonId <= 0) {
                throw new IllegalArgumentException(
                        "Invalid lesson ID in removedLessonIds");
            }

            lessonRepository
                    .findByCourseProgressIdAndLessonId(
                            progress.getId(),
                            lessonId
                    )
                    .ifPresent(lesson -> {

                        // Không xóa dữ liệu lịch sử
                        lesson.setIncludedInCurriculum(false);

                        lessonRepository.save(lesson);
                    });
        }
    }
}