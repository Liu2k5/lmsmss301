package com.example.user_service.dto;

import jakarta.validation.constraints.*;

public record UpdateProfileRequest(
    @NotBlank
    @Size(max = 128)
    String fullname,

    @Size(max = 15)
    String phone,

    Long universityId
) {}