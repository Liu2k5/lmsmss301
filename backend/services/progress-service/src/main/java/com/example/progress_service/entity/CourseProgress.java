package com.example.progress_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "course_progress",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"student_id", "course_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "course_id", nullable = false)
    private Integer courseId;

    @Column(name = "completion_percentage",
            precision = 5, scale = 2)
    private BigDecimal completionPercentage;

    @Column(name = "completed_lessons")
    private Integer completedLessons;

    @Column(name = "total_lessons")
    private Integer totalLessons;

    @Column(name = "average_quiz_score",
            precision = 5, scale = 2)
    private BigDecimal averageQuizScore;

    @Column(name = "last_lesson_id")
    private Integer lastLessonId;

    @Column(name = "last_watch_position")
    private Integer lastWatchPosition;

    @Column(name = "enrolled_at")
    private LocalDateTime enrolledAt;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "curriculum_version", nullable = false)
    @Builder.Default
    private Long curriculumVersion = 1L;

    @PrePersist
    public void beforeInsert() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (enrolledAt == null) {
            enrolledAt = now;
        }

        if (completionPercentage == null) {
            completionPercentage = BigDecimal.ZERO;
        }

        if (completedLessons == null) {
            completedLessons = 0;
        }

        if (totalLessons == null) {
            totalLessons = 0;
        }

        if (lastWatchPosition == null) {
            lastWatchPosition = 0;
        }

        if (status == null) {
            status = "IN_PROGRESS";
        }
    }

    @PreUpdate
    public void beforeUpdate() {
        updatedAt = LocalDateTime.now();
    }
}