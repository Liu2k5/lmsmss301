package com.example.progress_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "quiz_attempts",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {
                                "course_progress_id",
                                "quiz_id",
                                "attempt_number"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_progress_id", nullable = false)
    private CourseProgress courseProgress;

    @Column(name = "quiz_id", nullable = false)
    private Integer quizId;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    @Column(nullable = false)
    private Boolean passed;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @PrePersist
    public void beforeInsert() {
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
    }
}