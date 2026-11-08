package com.example.review_rating_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportReviewRequest {

    @NotBlank(message = "Lý do báo cáo không được để trống")
    @Size(max = 500, message = "Lý do báo cáo tối đa 500 ký tự")
    private String reason;
}
