package com.example.progress_service.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeLearningResponse {

    private Integer courseId;

    private Integer lessonId;

    private Integer watchPosition;

    private Boolean completed;
}