package com.example.review_rating_service.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentStatusDto {
    private String status; // ACTIVE, DROPPED, COMPLETED
    private LocalDateTime enrolledAt;

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }
}
