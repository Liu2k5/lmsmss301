package com.example.course_service.exception;

/** Xung đột dữ liệu: trùng khoá hoặc vi phạm ràng buộc duy nhất. */
public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}
}
