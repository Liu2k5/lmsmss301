package com.example.progress_service.entity;

import java.util.UUID;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "progress_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgressEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @PrePersist
    public void beforeInsert() {
        if (processedAt == null) {
            processedAt = LocalDateTime.now();
        }
    }
}