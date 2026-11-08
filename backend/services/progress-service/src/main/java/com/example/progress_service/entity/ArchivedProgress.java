
package com.example.progress_service.entity;

import java.util.UUID;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "archived_progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArchivedProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(
            name = "completion_percentage",
            nullable = false,
            precision = 5,
            scale = 2
    )
    private BigDecimal completionPercentage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "archived_data",
            nullable = false,
            columnDefinition = "jsonb"
    )
    private String archivedData;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @PrePersist
    public void beforeInsert() {
        if (archivedAt == null) {
            archivedAt = LocalDateTime.now();
        }
    }
}
