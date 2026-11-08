package com.example.review_rating_service.dto;

import com.example.review_rating_service.entity.ReviewStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReviewStatusRequest {

    @NotNull(message = "Trạng thái không được để trống")
    private ReviewStatus status;
}
