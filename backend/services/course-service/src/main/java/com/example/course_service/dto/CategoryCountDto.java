package com.example.course_service.dto;

import java.util.UUID;

/**
 * Một dòng kết quả đếm: category nào có bao nhiêu khoá học.
 * Được tạo trực tiếp trong câu JPQL bằng {@code select new ...CategoryCountDto(...)}.
 */
public record CategoryCountDto(UUID categoryId, Long total) {
}
