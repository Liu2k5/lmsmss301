package com.example.user_service.dto;

import java.time.Instant;

import com.example.user_service.entity.AccountRequestStatus;
import com.example.user_service.entity.AccountRequestType;

public record AccountRequestResponse(
        Long id,
        String username,
        String fullname,
        String email,
        AccountRequestType type,
        AccountRequestStatus status,
        Instant createdAt,
        Instant reviewedAt,
        String reviewedBy,
        String adminNote) {
}
