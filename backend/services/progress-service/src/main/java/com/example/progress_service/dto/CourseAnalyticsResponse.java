package com.example.progress_service.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseAnalyticsResponse {

    private Integer courseId;

    private Integer totalStudents;

    private Integer completedStudents;

    private BigDecimal completionRate;

    private BigDecimal averageQuizScore;
}