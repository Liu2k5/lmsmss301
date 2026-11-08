package com.example.progress_service.event;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuizSubmittedEvent {

    private String eventId;

    private Integer studentId;

    private Integer courseId;

    private Integer quizId;

    private BigDecimal score;

    private BigDecimal passingScore;

    private Integer attemptNumber;
}