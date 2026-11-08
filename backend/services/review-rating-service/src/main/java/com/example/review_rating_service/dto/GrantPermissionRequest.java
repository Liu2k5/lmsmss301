package com.example.review_rating_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrantPermissionRequest {

    @NotNull(message = "studentId không được để trống")
    private UUID studentId;

    @NotNull(message = "courseId không được để trống")
    private UUID courseId;
}
