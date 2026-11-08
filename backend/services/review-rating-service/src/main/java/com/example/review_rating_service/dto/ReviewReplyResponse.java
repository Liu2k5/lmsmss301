package com.example.review_rating_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewReplyResponse {
    private Long id;
    private UUID instructorId;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
