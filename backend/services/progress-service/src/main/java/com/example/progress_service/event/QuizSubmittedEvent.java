package com.example.progress_service.event;

import java.util.UUID;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuizSubmittedEvent {

    private String eventId;

    private UUID studentId;

    private UUID courseId;

    private UUID quizId;

    private BigDecimal score;

    private BigDecimal passingScore;

    private Integer attemptNumber;
}