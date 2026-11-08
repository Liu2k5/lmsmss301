package com.example.progress_service.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseProgressResponse {

    private Integer studentId;
    private Integer courseId;

    private BigDecimal completionPercentage;

    private Integer completedLessons;
    private Integer totalLessons;

    private BigDecimal averageQuizScore;

    private String status;

    private List<LessonProgressResponse> lessons;

    private LocalDateTime lastAccessedAt;
}