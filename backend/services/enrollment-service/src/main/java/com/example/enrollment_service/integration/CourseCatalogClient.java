package com.example.enrollment_service.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Lấy danh sách bài học đã xuất bản của khoá học từ Course Service (nội bộ, không qua Gateway). */
@Slf4j
@Component
public class CourseCatalogClient {

	private final RestClient rest;
	private final ObjectMapper objectMapper;

	public CourseCatalogClient(@Value("${app.services.course.url:http://localhost:8081}") String courseServiceUrl,
							   ObjectMapper objectMapper) {
		this.rest = RestClient.builder().baseUrl(courseServiceUrl).build();
		this.objectMapper = objectMapper;
	}

	/** Trả về rỗng nếu khoá học chưa xuất bản (404) hoặc Course Service không sẵn sàng. */
	public List<UUID> publishedLessonIds(UUID courseId) {
		try {
			String body = rest.get()
					.uri("/api/v1/courses/{courseId}/curriculum/published", courseId)
					.retrieve()
					.body(String.class);
			JsonNode root = objectMapper.readTree(body);
			List<UUID> ids = new ArrayList<>();
			for (JsonNode section : root.path("sections")) {
				for (JsonNode lesson : section.path("lessons")) {
					if (!lesson.path("id").isMissingNode()) {
						ids.add(UUID.fromString(lesson.path("id").asString()));
					}
				}
			}
			return ids;
		} catch (RuntimeException e) {
			log.warn("Không lấy được curriculum của khoá {}: {}", courseId, e.getMessage());
			return List.of();
		}
	}
}
