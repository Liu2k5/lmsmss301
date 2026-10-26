package com.example.course_service.exception;

/** Chi tiết lỗi của một field khi validate request. */
public record FieldErrorDetail(String field, String message) {
}
