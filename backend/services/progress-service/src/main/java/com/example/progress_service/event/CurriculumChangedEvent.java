package com.example.progress_service.event;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurriculumChangedEvent {

    // ID duy nhất của event
    private String eventId;

    // ID khóa học
    private UUID courseId;

    // Phiên bản curriculum sau khi cập nhật
    private Long curriculumVersion;

    // Các bài học mới được thêm
    private List<UUID> addedLessonIds;

    // Các bài học bị loại khỏi curriculum
    private List<UUID> removedLessonIds;
}