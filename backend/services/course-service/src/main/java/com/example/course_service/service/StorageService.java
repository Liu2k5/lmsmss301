package com.example.course_service.service;

import org.springframework.web.multipart.MultipartFile;

/** Kho lưu file học liệu (video, tài liệu, hình ảnh). Bản cài đặt hiện tại dùng MinIO. */
public interface StorageService {

	StoredFile upload(String folder, MultipartFile file);

	/** Link tạm (presigned URL) để trình duyệt xem / tải file, hết hạn sau thời gian cấu hình. */
	String viewUrl(String objectKey);

	void delete(String objectKey);

	record StoredFile(String objectKey, long size, String contentType) {
	}
}
