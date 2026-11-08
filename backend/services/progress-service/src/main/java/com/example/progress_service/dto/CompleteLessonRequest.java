package com.example.progress_service.dto;

import java.util.UUID;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompleteLessonRequest {

    private UUID studentId;

    private UUID courseId;
}