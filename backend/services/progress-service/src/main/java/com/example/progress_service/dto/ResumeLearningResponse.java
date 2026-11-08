package com.example.progress_service.dto;

import java.util.UUID;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeLearningResponse {

    private UUID courseId;

    private UUID lessonId;

    private Integer watchPosition;

    private Boolean completed;
}