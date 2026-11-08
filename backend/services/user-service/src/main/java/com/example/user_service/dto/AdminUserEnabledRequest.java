package com.example.user_service.dto;

import jakarta.validation.constraints.NotNull;

public record AdminUserEnabledRequest(@NotNull Boolean enabled) {
}
