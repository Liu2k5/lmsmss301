package com.example.course_service.client;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Hỏi Enrollment Service xem học viên có được xem nội dung khoá học không (UC-C30).
 * Gọi endpoint nội bộ {@code GET /api/v1/enrollments/check} (Gateway chặn endpoint này từ bên ngoài).
 */
@Component
public class EnrollmentClient {

	private static final Logger log = LoggerFactory.getLogger(EnrollmentClient.class);

	private final RestClient rest;

	public EnrollmentClient(@Value("${app.services.enrollment.url:http://localhost:8083}") String enrollmentUrl) {
		this.rest = RestClient.builder().baseUrl(enrollmentUrl).build();
	}

	/**
	 * true nếu học viên đang đăng ký khoá học và còn quyền truy cập (ACTIVE, chưa hết hạn).
	 * Không gọi được Enrollment Service → trả false (khoá nội dung cho an toàn).
	 */
	public boolean hasAccess(UUID studentId, UUID courseId) {
		try {
			CheckResponse response = rest.get()
					.uri("/api/v1/enrollments/check?studentId={studentId}&courseId={courseId}", studentId, courseId)
					.retrieve()
					.body(CheckResponse.class);
			if (response == null) {
				return false;
			}
			return response.accessGranted();
		}
		catch (RuntimeException ex) {
			log.warn("Không kiểm tra được đăng ký của {} với khoá {}: {}", studentId, courseId, ex.getMessage());
			return false;
		}
	}

	/** Phần cần dùng trong kết quả của Enrollment Service. */
	record CheckResponse(boolean enrolled, String status, boolean accessGranted) {
	}
}
