package com.example.review_rating_service.client;

import java.math.BigDecimal;

/**
 * Phản hồi của {@code GET /api/v1/progress/courses/{courseId}?studentId=} (Progress Service).
 * Chỉ cần hai trường dùng cho guard "hoàn thành khoá học" (UC-R01).
 */
public record ProgressServiceResponse(BigDecimal completionPercentage, String status) {
}
