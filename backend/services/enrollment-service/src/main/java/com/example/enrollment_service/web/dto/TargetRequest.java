package com.example.enrollment_service.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TargetRequest(@NotNull UUID studentId, @NotNull UUID courseId) {
}
