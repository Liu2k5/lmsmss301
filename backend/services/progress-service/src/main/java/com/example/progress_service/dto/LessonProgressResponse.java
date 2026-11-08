package com.example.progress_service.dto;

import java.util.UUID;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonProgressResponse {

    private UUID lessonId;

    private Boolean completed;

    private Integer watchPosition;

    private Integer videoDuration;
}