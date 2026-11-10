package com.example.review_rating_service.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentStatusDto {
    private Boolean enrolled;
    private String status; // ACTIVE, DROPPED, COMPLETED
    private Boolean accessGranted;
    private LocalDateTime enrolledAt;
    private Instant expiresAt;

    public boolean isActive() {
        if (Boolean.FALSE.equals(enrolled)) {
            return false;
        }
        if (Boolean.TRUE.equals(accessGranted)) {
            return true;
        }
        return "ACTIVE".equalsIgnoreCase(status);
    }
}
