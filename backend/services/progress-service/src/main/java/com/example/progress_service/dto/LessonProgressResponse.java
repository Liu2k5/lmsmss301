package com.example.progress_service.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonProgressResponse {

    private Integer lessonId;

    private Boolean completed;

    private Integer watchPosition;

    private Integer videoDuration;
}