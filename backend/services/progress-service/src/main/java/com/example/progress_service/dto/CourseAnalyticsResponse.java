package com.example.progress_service.dto;

import java.util.UUID;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseAnalyticsResponse {

    private UUID courseId;

    private Integer totalStudents;

    private Integer completedStudents;

    private BigDecimal completionRate;

    private BigDecimal averageQuizScore;
}