package com.example.course_service.dto;

import java.util.UUID;
import com.example.course_service.entity.MaterialType;

public record MaterialResponse(
		UUID id,
		UUID lessonId,
		String name,
		MaterialType type,
		/** Link xem file. Với file trên MinIO đây là link tạm (presigned), hết hạn sau vài giờ. */
		String url,
		/** true = file được upload lên MinIO; false = học liệu dạng link ngoài. */
		boolean uploaded,
		Long fileSize,
		String mimeType,
		Integer position) {
}
