package com.example.progress_service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompleteLessonRequest {

    private Integer studentId;

    private Integer courseId;
}