package com.example.course_service.exception;

/** Dữ liệu hợp lệ về cú pháp nhưng vi phạm quy tắc nghiệp vụ (ví dụ: quiz không có đáp án đúng). */
public class BusinessRuleException extends RuntimeException {

	public BusinessRuleException(String message) {
		super(message);
	}
}
