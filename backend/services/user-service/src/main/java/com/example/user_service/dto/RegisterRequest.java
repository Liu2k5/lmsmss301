package com.example.user_service.dto;

import com.example.user_service.entity.Role;
import jakarta.validation.constraints.*;

public record RegisterRequest(
    @NotBlank
    @Size(max = 20)
    String username,

    @NotBlank
    @Size(max = 128)
    String fullname,

    @NotBlank
    @Email
    @Size(max = 128)
    String email,

    @NotBlank
    @Size(min = 8, max = 72)
    String password,

    @Size(max = 15)
    String phone,

    Role role,

    Long universityId
) {}