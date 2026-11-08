package com.example.user_service.dto;

import com.example.user_service.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminUserUpdateRequest(
        @NotBlank @Size(max = 128) String fullname,
        @Size(max = 15) String phone,
        @NotNull Role role,
        Long universityId,
        @NotNull Boolean enabled) {
}
