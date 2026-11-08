package com.example.enrollment_service.web.dto;

public record ErrorResponse(String code, String message, Object details) {
}
