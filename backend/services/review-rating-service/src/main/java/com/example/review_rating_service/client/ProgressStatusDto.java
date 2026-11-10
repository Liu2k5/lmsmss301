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
    private BigDecimal completionPercentage;
    private Integer completedLessons;
    private Integer totalLessons;
    private String status;
    private Boolean courseCompleted;

    public boolean isCompleted() {
        if ("COMPLETED".equalsIgnoreCase(status) || Boolean.TRUE.equals(courseCompleted)) {
            return true;
        }
        return completionPercentage != null && completionPercentage.compareTo(new BigDecimal("100.00")) >= 0;
    }

    public BigDecimal getPercentComplete() {
        return this.completionPercentage;
    }

    public void setPercentComplete(BigDecimal percentComplete) {
        this.completionPercentage = percentComplete;
    }

    public static class ProgressStatusDtoBuilder {
        public ProgressStatusDtoBuilder percentComplete(BigDecimal percentComplete) {
            this.completionPercentage = percentComplete;
            return this;
        }
    }
}
