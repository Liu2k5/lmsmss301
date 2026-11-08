package com.example.progress_service.service;

import com.example.progress_service.entity.*;
import com.example.progress_service.event.CurriculumChangedEvent;
import com.example.progress_service.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CurriculumProgressService {

    private final CourseProgressRepository progressRepository;

    private final LessonProgressRepository lessonRepository;

    private final ProgressEventRepository eventRepository;

    private final ProgressService progressService;

    @Transactional
    public void handleCurriculumChanged(
            CurriculumChangedEvent event) {

        if (eventRepository.existsByEventId(event.getEventId())) {
            return;
        }

        List<CourseProgress> progresses =
                progressRepository.findByCourseId(
                        event.getCourseId()
                );

        for (CourseProgress progress : progresses) {

            // Thêm lesson mới
            if (event.getAddedLessonIds() != null) {

                for (UUID lessonId :
                        event.getAddedLessonIds()) {

                    boolean exists =
                            lessonRepository
                                    .existsByCourseProgressIdAndLessonId(
                                            progress.getId(),
                                            lessonId
                                    );

                    if (!exists) {

                        LessonProgress lesson =
                                LessonProgress.builder()
                                        .courseProgress(progress)
                                        .lessonId(lessonId)
                                        .completed(false)
                                        .watchPosition(0)
                                        .videoDuration(0)
                                        .build();

                        lessonRepository.save(lesson);
                    }
                }
            }

            // Xóa lesson khỏi curriculum
            if (event.getRemovedLessonIds() != null) {

                for (UUID lessonId :
                        event.getRemovedLessonIds()) {

                    lessonRepository
                            .deleteByCourseProgressIdAndLessonId(
                                    progress.getId(),
                                    lessonId
                            );
                }
            }

            // Tính lại tiến độ
            progressService.recalculateProgress(
                    progress.getId()
            );
        }

        eventRepository.save(
                ProgressEvent.builder()
                        .eventId(event.getEventId())
                        .eventType("CurriculumChanged")
                        .build()
        );
    }
}