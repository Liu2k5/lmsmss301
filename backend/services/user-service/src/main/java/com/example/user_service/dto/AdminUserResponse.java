package com.example.user_service.dto;

import com.example.user_service.entity.Role;

public record AdminUserResponse(
        String username,
        String fullname,
        String email,
        String phone,
        Role role,
        Long universityId,
        String universityName,
        boolean enabled,
        boolean deleted) {
}
