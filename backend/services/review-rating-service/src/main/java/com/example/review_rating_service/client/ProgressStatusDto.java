package com.example.review_rating_service.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressStatusDto {
    private UUID studentId;
    private UUID courseId;
    private BigDecimal percentComplete;
    private Boolean courseCompleted;

    public boolean isCompleted() {
        return Boolean.TRUE.equals(courseCompleted) ||
                (percentComplete != null && percentComplete.compareTo(new BigDecimal("100.00")) >= 0);
    }
}
