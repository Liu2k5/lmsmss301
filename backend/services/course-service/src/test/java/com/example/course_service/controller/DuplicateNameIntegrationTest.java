package com.example.course_service.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.example.course_service.entity.Course;
import com.example.course_service.entity.CourseStatus;
import com.example.course_service.repository.CourseRepository;
import com.example.course_service.support.TestLogin;

import tools.jackson.databind.ObjectMapper;

/**
 * Không cho trùng tên:
 * <ul>
 *   <li>khoá học của cùng một giảng viên</li>
 *   <li>chương trong cùng một khoá học</li>
 *   <li>bài học trong cùng một chương</li>
 * </ul>
 * So sánh không phân biệt hoa thường và bỏ khoảng trắng đầu / cuối.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) // test nghiệp vụ: phân quyền có test riêng
@Transactional
class DuplicateNameIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private CourseRepository courseRepository;

	@BeforeEach
	void loginAsInstructor() {
		TestLogin.asInstructor();
	}

	@AfterEach
	void logout() {
		TestLogin.logout();
	}

	// ================================================================ khoá học

	@Test
	void giangVienKhongTaoDuoc2KhoaHocTrungTen() throws Exception {
		createCourse("Java cơ bản");

		// Khác hoa thường + thừa khoảng trắng vẫn tính là trùng
		send(post("/api/v1/courses"), courseJson("  JAVA CƠ BẢN "))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Bạn đã có khoá học tên 'JAVA CƠ BẢN'. Hãy đặt tên khác."));
	}

	@Test
	void giangVienKhacDuocDatCungTenKhoaHoc() throws Exception {
		// Khoá học "Java cơ bản" của một giảng viên khác
		courseRepository.saveAndFlush(Course.builder()
				.instructorId(UUID.randomUUID())
				.title("Java cơ bản")
				.status(CourseStatus.DRAFT)
				.build());

		send(post("/api/v1/courses"), courseJson("Java cơ bản")).andExpect(status().isCreated());
	}

	@Test
	void suaKhoaHocGiuNguyenTenCuaChinhNoThiDuoc() throws Exception {
		String id = createCourse("Java cơ bản");

		send(put("/api/v1/courses/{id}", id), courseJson("java cơ bản"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("java cơ bản"));
	}

	@Test
	void suaKhoaHocTrungTenKhoaKhacCuaMinhThiBiChan() throws Exception {
		createCourse("Java cơ bản");
		String other = createCourse("Spring Boot");

		send(put("/api/v1/courses/{id}", other), courseJson("Java cơ bản"))
				.andExpect(status().isConflict());
	}

	// ================================================================== chương

	@Test
	void khongThemDuoc2ChuongTrungTenTrongCungKhoaHoc() throws Exception {
		String courseId = createCourse("Java cơ bản");
		addSection(courseId, "Chương 1: Giới thiệu").andExpect(status().isCreated());

		addSection(courseId, "chương 1: giới thiệu ")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message")
						.value("Khoá học đã có chương tên 'chương 1: giới thiệu'. Hãy đặt tên khác."));
	}

	@Test
	void haiKhoaHocKhacNhauDuocCoChuongCungTen() throws Exception {
		String java = createCourse("Java cơ bản");
		String spring = createCourse("Spring Boot");
		addSection(java, "Chương 1: Giới thiệu").andExpect(status().isCreated());

		addSection(spring, "Chương 1: Giới thiệu").andExpect(status().isCreated());
	}

	@Test
	void suaChuongTrungTenChuongKhacThiBiChan() throws Exception {
		String courseId = createCourse("Java cơ bản");
		addSection(courseId, "Chương 1").andExpect(status().isCreated());
		String detail = addSection(courseId, "Chương 2").andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		String chapter2Id = objectMapper.readTree(detail).get("sections").get(1).get("id").asText();

		// Đổi tên chương 2 thành "Chương 1" → trùng
		send(put("/api/v1/sections/{id}", chapter2Id), "{\"title\": \"CHƯƠNG 1\"}")
				.andExpect(status().isConflict());
		// Giữ nguyên tên của chính nó (chỉ đổi hoa thường) → được
		send(put("/api/v1/sections/{id}", chapter2Id), "{\"title\": \"chương 2\"}")
				.andExpect(status().isOk());
	}

	// ================================================================= bài học

	@Test
	void khongThemDuoc2BaiHocTrungTenTrongCungChuong() throws Exception {
		String sectionId = firstSectionId(createCourse("Java cơ bản"), "Chương 1");
		addLesson(sectionId, "Bài 1: Biến").andExpect(status().isCreated());

		addLesson(sectionId, " BÀI 1: BIẾN ")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Chương đã có bài học tên 'BÀI 1: BIẾN'. Hãy đặt tên khác."));
	}

	@Test
	void haiChuongKhacNhauDuocCoBaiHocCungTen() throws Exception {
		String courseId = createCourse("Java cơ bản");
		String chapter1 = firstSectionId(courseId, "Chương 1");
		String detail = addSection(courseId, "Chương 2").andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		String chapter2 = objectMapper.readTree(detail).get("sections").get(1).get("id").asText();

		addLesson(chapter1, "Ôn tập").andExpect(status().isCreated());
		addLesson(chapter2, "Ôn tập").andExpect(status().isCreated());
	}

	@Test
	void suaBaiHocTrungTenBaiKhacThiBiChan() throws Exception {
		String sectionId = firstSectionId(createCourse("Java cơ bản"), "Chương 1");
		addLesson(sectionId, "Bài 1").andExpect(status().isCreated());
		String detail = addLesson(sectionId, "Bài 2").andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		String lesson2Id = objectMapper.readTree(detail)
				.get("sections").get(0).get("lessons").get(1).get("id").asText();

		// Đổi tên bài 2 thành "Bài 1" → trùng
		send(put("/api/v1/lessons/{id}", lesson2Id), lessonJson("bài 1"))
				.andExpect(status().isConflict());
		// Giữ nguyên tên của chính nó (chỉ đổi hoa thường) → được
		send(put("/api/v1/lessons/{id}", lesson2Id), lessonJson("BÀI 2"))
				.andExpect(status().isOk());
	}

	// ================================================================ hàm phụ

	private String createCourse(String title) throws Exception {
		String body = send(post("/api/v1/courses"), courseJson(title))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(body).get("id").asText();
	}

	private ResultActions addSection(String courseId, String title) throws Exception {
		return send(post("/api/v1/courses/{id}/sections", courseId), "{\"title\": \"" + title + "\"}");
	}

	/** Thêm 1 chương vào khoá học và trả về id của chương đầu tiên. */
	private String firstSectionId(String courseId, String title) throws Exception {
		String detail = addSection(courseId, title).andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(detail).get("sections").get(0).get("id").asText();
	}

	private ResultActions addLesson(String sectionId, String title) throws Exception {
		return send(post("/api/v1/sections/{id}/lessons", sectionId), lessonJson(title));
	}

	private String lessonJson(String title) {
		return """
				{"title": "%s", "type": "VIDEO"}
				""".formatted(title);
	}

	private String courseJson(String title) {
		return """
				{"instructorId": "%s", "title": "%s"}
				""".formatted(TestLogin.INSTRUCTOR_ID, title);
	}

	private ResultActions send(MockHttpServletRequestBuilder builder, String body) throws Exception {
		return mockMvc.perform(builder.contentType(MediaType.APPLICATION_JSON).content(body));
	}
}
