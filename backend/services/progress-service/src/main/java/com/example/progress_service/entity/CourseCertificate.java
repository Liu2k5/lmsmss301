package com.example.progress_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "course_certificates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseCertificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_progress_id", nullable = false)
    private CourseProgress courseProgress;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "course_id", nullable = false)
    private Integer courseId;

    @Column(name = "certificate_code",
            nullable = false, unique = true)
    private String certificateCode;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @PrePersist
    public void beforeInsert() {
        if (issuedAt == null) {
            issuedAt = LocalDateTime.now();
        }
    }
}