package com.example.course_service.exception;

/** Lỗi khi làm việc với kho lưu file (MinIO): không kết nối được, upload / xoá thất bại. */
public class StorageException extends RuntimeException {

	public StorageException(String message, Throwable cause) {
		super(message, cause);
	}
}
