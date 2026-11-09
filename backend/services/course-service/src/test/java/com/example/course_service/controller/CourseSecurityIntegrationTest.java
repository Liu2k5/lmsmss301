package com.example.course_service.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.example.course_service.entity.Course;
import com.example.course_service.entity.CourseStatus;
import com.example.course_service.repository.CourseRepository;

/**
 * Phân quyền soạn nội dung khoá học (UC-C01…C13):
 * chỉ giảng viên phụ trách khoá học (và Admin) được sửa.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CourseSecurityIntegrationTest {

	private static final String INSTRUCTOR = "22222222-2222-2222-2222-222222222222";
	private static final String OTHER_INSTRUCTOR = "44444444-4444-4444-4444-444444444444";
	private static final String STUDENT = "11111111-1111-1111-1111-111111111111";
	private static final String ADMIN = "33333333-3333-3333-3333-333333333333";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CourseRepository courseRepository;

	private UUID courseId;

	@BeforeEach
	void setUp() {
		Course course = courseRepository.save(Course.builder()
				.instructorId(UUID.fromString(INSTRUCTOR))
				.title("Khoá học của giảng viên A")
				.status(CourseStatus.DRAFT)
				.build());
		courseId = course.getId();
	}

	// ------------------------------------------------------------- đọc công khai

	@Test
	void khachVanXemDuocCatalog() throws Exception {
		mockMvc.perform(get("/api/v1/courses")).andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/courses/" + courseId)).andExpect(status().isOk());
	}

	// ------------------------------------------------------------- chưa đăng nhập

	@Test
	void chuaDangNhapKhongTaoDuocKhoaHoc() throws Exception {
		mockMvc.perform(post("/api/v1/courses").contentType(MediaType.APPLICATION_JSON).content(courseJson(INSTRUCTOR)))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(put("/api/v1/courses/" + courseId).contentType(MediaType.APPLICATION_JSON)
						.content(courseJson(INSTRUCTOR)))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(delete("/api/v1/courses/" + courseId)).andExpect(status().isUnauthorized());
	}

	// ------------------------------------------------------------- học viên

	@Test
	void hocVienKhongSuaDuocNoiDung() throws Exception {
		mockMvc.perform(as(post("/api/v1/courses"), STUDENT, "student")
						.contentType(MediaType.APPLICATION_JSON).content(courseJson(STUDENT)))
				.andExpect(status().isForbidden());
		mockMvc.perform(as(put("/api/v1/courses/" + courseId), STUDENT, "student")
						.contentType(MediaType.APPLICATION_JSON).content(courseJson(INSTRUCTOR)))
				.andExpect(status().isForbidden());
		mockMvc.perform(as(patch("/api/v1/courses/" + courseId + "/status"), STUDENT, "student")
						.contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ARCHIVED\"}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(as(post("/api/v1/courses/" + courseId + "/sections"), STUDENT, "student")
						.contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Chương lạ\"}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(as(delete("/api/v1/courses/" + courseId), STUDENT, "student"))
				.andExpect(status().isForbidden());
	}

	// ------------------------------------------------------------- giảng viên

	@Test
	void giangVienSoHuuSuaDuocKhoaHoc() throws Exception {
		mockMvc.perform(as(put("/api/v1/courses/" + courseId), INSTRUCTOR, "instructor")
						.contentType(MediaType.APPLICATION_JSON).content(courseJson(INSTRUCTOR)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Khoá học mới"));

		mockMvc.perform(as(post("/api/v1/courses/" + courseId + "/sections"), INSTRUCTOR, "instructor")
						.contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Chương 1\"}"))
				.andExpect(status().isCreated());
	}

	@Test
	void giangVienKhongSuaDuocKhoaHocCuaNguoiKhac() throws Exception {
		mockMvc.perform(as(put("/api/v1/courses/" + courseId), OTHER_INSTRUCTOR, "instructor")
						.contentType(MediaType.APPLICATION_JSON).content(courseJson(OTHER_INSTRUCTOR)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value(
						"Chỉ giảng viên phụ trách khoá học mới được chỉnh sửa nội dung khoá học này"));

		mockMvc.perform(as(delete("/api/v1/courses/" + courseId), OTHER_INSTRUCTOR, "instructor"))
				.andExpect(status().isForbidden());
		mockMvc.perform(as(post("/api/v1/courses/" + courseId + "/sections"), OTHER_INSTRUCTOR, "instructor")
						.contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Chen ngang\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void giangVienChiTaoDuocKhoaHocChoChinhMinh() throws Exception {
		mockMvc.perform(as(post("/api/v1/courses"), INSTRUCTOR, "instructor")
						.contentType(MediaType.APPLICATION_JSON)
						.content(courseJson(OTHER_INSTRUCTOR)))
				.andExpect(status().isForbidden());

		mockMvc.perform(as(post("/api/v1/courses"), INSTRUCTOR, "instructor")
						.contentType(MediaType.APPLICATION_JSON).content(courseJson(INSTRUCTOR)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.instructorId").value(INSTRUCTOR));
	}

	@Test
	void adminQuanLyDuocMoiKhoaHoc() throws Exception {
		mockMvc.perform(as(put("/api/v1/courses/" + courseId), ADMIN, "admin")
						.contentType(MediaType.APPLICATION_JSON).content(courseJson(INSTRUCTOR)))
				.andExpect(status().isOk());
		mockMvc.perform(as(delete("/api/v1/courses/" + courseId), ADMIN, "admin"))
				.andExpect(status().isNoContent());
	}

	// ------------------------------------------------------------- helpers

	private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder builder, String subject, String role) {
		JwtRequestPostProcessor jwt = jwt()
				.jwt(j -> j.subject(subject).claim("realm_access", Map.of("roles", List.of(role))))
				.authorities(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
		return builder.with(jwt);
	}

	private String courseJson(String instructorId) {
		return """
				{"instructorId":"%s","title":"Khoá học mới","description":"test",
				 "thumbnailUrl":"https://placehold.co/600x340"}
				""".formatted(instructorId);
	}
}
