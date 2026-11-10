package com.example.course_service.controller;

import com.example.course_service.support.TestLogin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.example.course_service.config.CourseDemoDataSeeder;
import com.example.course_service.repository.CategoryRepository;
import com.example.course_service.repository.CourseRepository;
import com.example.course_service.repository.LessonRepository;
import com.example.course_service.repository.MaterialRepository;
import com.example.course_service.repository.QuizRepository;
import com.example.course_service.repository.SectionRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) // test nghiệp vụ: bỏ qua xác thực, phần quyền có test riêng
@Transactional
class CourseControllerIntegrationTest {

	/** Test nghiệp vụ tắt filter bảo mật → tự đăng nhập bằng giảng viên chủ các khoá học trong test. */
	@BeforeEach
	void loginAsInstructor() {
		TestLogin.asInstructor();
	}

	@AfterEach
	void logout() {
		TestLogin.logout();
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private CourseRepository courseRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private SectionRepository sectionRepository;

	@Autowired
	private LessonRepository lessonRepository;

	@Autowired
	private MaterialRepository materialRepository;

	@Autowired
	private QuizRepository quizRepository;

	@Test
	void taoKhoaHocMoiLuonODraft() throws Exception {
		send(post("/api/v1/courses"), courseJson("Java cơ bản"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isString())
				.andExpect(jsonPath("$.status").value("DRAFT"))
				.andExpect(jsonPath("$.editable").value(true))
				.andExpect(jsonPath("$.allowedTransitions", hasSize(2)))
				.andExpect(jsonPath("$.sections", hasSize(0)));
	}

	@Test
	void taoKhoaHocThieuTenTraVe400() throws Exception {
		send(post("/api/v1/courses"), """
				{"instructorId": "22222222-2222-2222-2222-222222222222", "title": "  ", "thumbnailUrl": "ftp://x"}
				""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.details", hasSize(2)));
	}

	@Test
	void timKiemTheoTuKhoaVaTrangThai() throws Exception {
		createCourse("Spring Boot nâng cao");
		createCourse("ReactJS thực chiến");

		// Khoá mới tạo là bản nháp → tìm qua "Khoá học của tôi"
		mockMvc.perform(get("/api/v1/instructor/courses").param("keyword", "react"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].title").value("ReactJS thực chiến"));

		mockMvc.perform(get("/api/v1/instructor/courses").param("status", "PUBLISHED"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));

		// API công khai không trả bản nháp
		mockMvc.perform(get("/api/v1/courses").param("keyword", "react"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));

		mockMvc.perform(get("/api/v1/instructor/courses").param("status", "KHONG_TON_TAI"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void capNhatThongTinKhoaHoc() throws Exception {
		String id = createCourse("Tên cũ");
		send(put("/api/v1/courses/{id}", id), courseJson("Tên mới"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Tên mới"));

		mockMvc.perform(get("/api/v1/courses/{id}", java.util.UUID.randomUUID())).andExpect(status().isNotFound());
	}

	@Test
	void quanLyChuongBaiHocVaHocLieu() throws Exception {
		String courseId = createCourse("Khoá học curriculum");
		JsonNode detail = addSection(courseId, "Chương A");
		detail = addSection(courseId, "Chương B");
		String sectionA = detail.get("sections").get(0).get("id").asText();
		String sectionB = detail.get("sections").get(1).get("id").asText();

		addLesson(sectionA, "Bài 1", "VIDEO");
		detail = addLesson(sectionA, "Bài 2", "ARTICLE");
		String lesson1 = detail.get("sections").get(0).get("lessons").get(0).get("id").asText();
		String lesson2 = detail.get("sections").get(0).get("lessons").get(1).get("id").asText();

		// Học liệu
		send(post("/api/v1/lessons/{id}/materials", lesson1), """
				{"name": "Slide", "type": "DOCUMENT", "url": "https://cdn.local/a.pdf", "fileSize": 1024}
				""")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.sections[0].lessons[0].materials", hasSize(1)))
				.andExpect(jsonPath("$.sections[0].lessons[0].materials[0].position").value(1));

		send(post("/api/v1/lessons/{id}/materials", lesson1), """
				{"name": "Sai", "type": "DOCUMENT", "url": "not-a-url"}
				""")
				.andExpect(status().isBadRequest());

		// Đổi thứ tự chương & bài học
		send(put("/api/v1/courses/{id}/sections/order", courseId),
				"{\"orderedIds\": [\"" + sectionB + "\",\"" + sectionA + "\"]}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sections[0].title").value("Chương B"))
				.andExpect(jsonPath("$.sections[1].position").value(2));

		send(put("/api/v1/sections/{id}/lessons/order", sectionA),
				"{\"orderedIds\": [\"" + lesson2 + "\",\"" + lesson1 + "\"]}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sections[1].lessons[0].title").value("Bài 2"));

		send(put("/api/v1/sections/{id}/lessons/order", sectionA), "{\"orderedIds\": [\"" + lesson1 + "\"]}")
				.andExpect(status().isBadRequest());

		// Sửa bài học: đặt xem thử
		send(put("/api/v1/lessons/{id}", lesson2), """
				{"title": "Bài 2 (sửa)", "type": "ARTICLE", "preview": true}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sections[1].lessons[0].title").value("Bài 2 (sửa)"))
				.andExpect(jsonPath("$.sections[1].lessons[0].preview").value(true));

		// Xoá bài học có học liệu → học liệu bị xoá theo, vị trí được đánh lại
		mockMvc.perform(delete("/api/v1/lessons/{id}", lesson2)).andExpect(status().isOk());
		mockMvc.perform(delete("/api/v1/lessons/{id}", lesson1))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sections[1].lessons", hasSize(0)));
		org.assertj.core.api.Assertions.assertThat(materialRepository.count()).isZero();

		// Xoá chương → chương còn lại được đánh số lại
		mockMvc.perform(delete("/api/v1/sections/{id}", sectionB))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sections", hasSize(1)))
				.andExpect(jsonPath("$.sections[0].position").value(1));
	}

	@Test
	void khongDoiLoaiBaiHocKhiDaCoQuiz() throws Exception {
		String courseId = createCourse("Khoá có quiz");
		String sectionId = addSection(courseId, "Chương 1").get("sections").get(0).get("id").asText();
		String lessonId = addLesson(sectionId, "Kiểm tra", "QUIZ")
				.get("sections").get(0).get("lessons").get(0).get("id").asText();
		createQuiz(lessonId);

		mockMvc.perform(get("/api/v1/instructor/courses/{id}", courseId))
				.andExpect(jsonPath("$.sections[0].lessons[0].quizId").isString());

		send(put("/api/v1/lessons/{id}", lessonId), """
				{"title": "Kiểm tra", "type": "VIDEO"}
				""")
				.andExpect(status().isConflict());

		// Xoá bài học thì quiz cũng bị xoá
		mockMvc.perform(delete("/api/v1/lessons/{id}", lessonId)).andExpect(status().isOk());
		org.assertj.core.api.Assertions.assertThat(quizRepository.count()).isZero();
	}

	@Test
	void vongDoiTrangThaiKhoaHoc() throws Exception {
		String courseId = createCourse("Khoá vòng đời");

		// Chưa có chương → không xuất bản được
		changeStatus(courseId, "PUBLISHED")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", containsString("ít nhất 1 chương")));

		String sectionId = addSection(courseId, "Chương 1").get("sections").get(0).get("id").asText();
		changeStatus(courseId, "PUBLISHED")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", containsString("chưa có bài học")));

		addLesson(sectionId, "Bài 1", "VIDEO");

		// DRAFT không chuyển thẳng sang UNPUBLISHED; trạng thái duyệt cũ không còn tồn tại
		changeStatus(courseId, "UNPUBLISHED").andExpect(status().isConflict());
		changeStatus(courseId, "PENDING_REVIEW").andExpect(status().isBadRequest());

		// Giảng viên tự xuất bản, không qua bước duyệt
		changeStatus(courseId, "PUBLISHED").andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PUBLISHED"));

		// Đã xuất bản → có trong danh sách công khai
		mockMvc.perform(get("/api/v1/courses").param("keyword", "Khoá vòng đời"))
				.andExpect(jsonPath("$", hasSize(1)));

		mockMvc.perform(get("/api/v1/courses/{id}/curriculum/published", courseId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalLessons").value(1))
				.andExpect(jsonPath("$.sections[0].lessons[0].title").value("Bài 1"));

		// Đang PUBLISHED thì không xoá được
		mockMvc.perform(delete("/api/v1/courses/{id}", courseId)).andExpect(status().isConflict());

		changeStatus(courseId, "UNPUBLISHED").andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/courses/{id}/curriculum/published", courseId))
				.andExpect(status().isNotFound());

		// Đã ẩn → không còn trong danh sách công khai, kể cả khi lọc đúng trạng thái
		mockMvc.perform(get("/api/v1/courses").param("keyword", "Khoá vòng đời"))
				.andExpect(jsonPath("$", hasSize(0)));
		mockMvc.perform(get("/api/v1/courses").param("status", "UNPUBLISHED"))
				.andExpect(jsonPath("$", hasSize(0)));

		changeStatus(courseId, "ARCHIVED").andExpect(status().isOk())
				.andExpect(jsonPath("$.editable").value(false));

		// Đã lưu trữ → cũng không hiện công khai; giảng viên vẫn thấy trong "Khoá học của tôi"
		mockMvc.perform(get("/api/v1/courses").param("status", "ARCHIVED"))
				.andExpect(jsonPath("$", hasSize(0)));
		mockMvc.perform(get("/api/v1/instructor/courses").param("status", "ARCHIVED").param("keyword", "Khoá vòng đời"))
				.andExpect(jsonPath("$", hasSize(1)));

		// Đã lưu trữ → không sửa được curriculum
		send(post("/api/v1/courses/{id}/sections", courseId), "{\"title\": \"Chương mới\"}")
				.andExpect(status().isBadRequest());

		// Lưu trữ thì xoá được, kéo theo chương & bài học
		mockMvc.perform(delete("/api/v1/courses/{id}", courseId)).andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/courses/{id}", courseId)).andExpect(status().isNotFound());
		org.assertj.core.api.Assertions.assertThat(sectionRepository.count()).isZero();
		org.assertj.core.api.Assertions.assertThat(lessonRepository.count()).isZero();
	}

	@Test
	void xuatBanKhiBaiQuizChuaCoQuizBiChan() throws Exception {
		String courseId = createCourse("Khoá thiếu quiz");
		String sectionId = addSection(courseId, "Chương 1").get("sections").get(0).get("id").asText();
		addLesson(sectionId, "Kiểm tra cuối khoá", "QUIZ");

		changeStatus(courseId, "PUBLISHED")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", containsString("chưa được tạo quiz")));
	}

	@Test
	void curriculumPublishedBoQuaBaiQuizChuaCoQuizVaChuongRong() throws Exception {
		String courseId = createCourse("Khoá đang xuất bản");
		String sectionA = addSection(courseId, "Chương A").get("sections").get(0).get("id").asText();
		addLesson(sectionA, "Bài video", "VIDEO");
		changeStatus(courseId, "PUBLISHED").andExpect(status().isOk());

		// Sau khi xuất bản, giảng viên thêm bài Quiz (chưa tạo quiz) và một chương rỗng.
		addLesson(sectionA, "Bài kiểm tra", "QUIZ");
		addSection(courseId, "Chương B rỗng");

		mockMvc.perform(get("/api/v1/courses/{id}/curriculum/published", courseId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalLessons").value(1))
				.andExpect(jsonPath("$.sections", hasSize(1)))
				.andExpect(jsonPath("$.sections[0].lessons", hasSize(1)))
				.andExpect(jsonPath("$.sections[0].lessons[0].title").value("Bài video"));
	}

	@Test
	void saiMethodVaSaiContentTypeTraDungMaLoi() throws Exception {
		mockMvc.perform(delete("/api/v1/courses"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.status").value(405));

		mockMvc.perform(post("/api/v1/courses").contentType(MediaType.TEXT_PLAIN).content("abc"))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.status").value(415));
	}

	@Test
	void seederChayNhieuLanKhongTaoTrung() throws Exception {
		CourseDemoDataSeeder seeder = new CourseDemoDataSeeder(
				courseRepository, categoryRepository, sectionRepository, lessonRepository, materialRepository);
		seeder.run(null);
		materialRepository.deleteAll(); // trước đây xoá hết học liệu là lần sau seed lại
		seeder.run(null);

		// Khoá ReactJS mẫu là bản nháp của giảng viên đang đăng nhập; khoá Docker đã xuất bản
		mockMvc.perform(get("/api/v1/instructor/courses").param("keyword", "ReactJS từ cơ bản"))
				.andExpect(jsonPath("$", hasSize(1)));
		mockMvc.perform(get("/api/v1/courses").param("keyword", "Docker cho lập trình viên"))
				.andExpect(jsonPath("$", hasSize(1)));
	}

	// --------------------------------------------------------------- helpers

	private ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder,
			String body) throws Exception {
		return mockMvc.perform(builder.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private ResultActions changeStatus(String courseId, String status) throws Exception {
		return send(patch("/api/v1/courses/{id}/status", courseId), "{\"status\": \"" + status + "\"}");
	}

	private String courseJson(String title) {
		return """
				{"instructorId": "22222222-2222-2222-2222-222222222222", "title": "%s", "description": "Mô tả",
				 "thumbnailUrl": "https://placehold.co/600x340"}
				""".formatted(title);
	}

	private String createCourse(String title) throws Exception {
		return read(send(post("/api/v1/courses"), courseJson(title)).andExpect(status().isCreated()))
				.get("id").asText();
	}

	private JsonNode addSection(String courseId, String title) throws Exception {
		return read(send(post("/api/v1/courses/{id}/sections", courseId), "{\"title\": \"" + title + "\"}")
				.andExpect(status().isCreated()));
	}

	private JsonNode addLesson(String sectionId, String title, String type) throws Exception {
		return read(send(post("/api/v1/sections/{id}/lessons", sectionId),
				"{\"title\": \"" + title + "\", \"type\": \"" + type + "\"}")
				.andExpect(status().isCreated()));
	}

	private void createQuiz(String lessonId) throws Exception {
		send(post("/api/v1/quizzes"), """
				{"lessonId": "%s", "title": "Quiz", "passingScore": 50,
				 "questions": [{"content": "1+1?", "type": "SINGLE_CHOICE",
				   "options": [{"content": "2", "correct": true}, {"content": "3", "correct": false}]}]}
				""".formatted(lessonId))
				.andExpect(status().isCreated());
	}

	private JsonNode read(ResultActions result) throws Exception {
		return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
	}
}
