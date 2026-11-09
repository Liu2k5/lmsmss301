package com.example.course_service.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.example.course_service.entity.Lesson;
import com.example.course_service.entity.LessonType;
import com.example.course_service.entity.Section;
import com.example.course_service.repository.CourseRepository;
import com.example.course_service.repository.LessonRepository;
import com.example.course_service.repository.QuizRepository;
import com.example.course_service.repository.SectionRepository;

/**
 * Phân quyền quiz theo bảng use case UC-C14…C19:
 * chỉ giảng viên phụ trách khoá học (và Admin) được tạo/sửa/xoá quiz và câu hỏi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class QuizSecurityIntegrationTest {

	private static final String INSTRUCTOR = "22222222-2222-2222-2222-222222222222";
	private static final String OTHER_INSTRUCTOR = "44444444-4444-4444-4444-444444444444";
	private static final String STUDENT = "11111111-1111-1111-1111-111111111111";
	private static final String ADMIN = "33333333-3333-3333-3333-333333333333";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CourseRepository courseRepository;

	@Autowired
	private SectionRepository sectionRepository;

	@Autowired
	private LessonRepository lessonRepository;

	@Autowired
	private QuizRepository quizRepository;

	private Lesson quizLesson;
	private Lesson spareQuizLesson;

	@BeforeEach
	void setUp() {
		quizLesson = createQuizLesson(INSTRUCTOR, "Bài kiểm tra 1");
		spareQuizLesson = createQuizLesson(OTHER_INSTRUCTOR, "Bài kiểm tra 2");
	}

	// ------------------------------------------------------------ chưa đăng nhập

	@Test
	void khachChuaDangNhapKhongXemDuocQuiz() throws Exception {
		mockMvc.perform(get("/api/v1/quizzes")).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/v1/quizzes").contentType(MediaType.APPLICATION_JSON).content(payload()))
				.andExpect(status().isUnauthorized());
	}

	// ------------------------------------------------------------ theo vai trò

	@Test
	void hocVienKhongDuocTaoSuaXoaQuiz() throws Exception {
		mockMvc.perform(as(post("/api/v1/quizzes"), STUDENT, "student").contentType(MediaType.APPLICATION_JSON)
						.content(payload()))
				.andExpect(status().isForbidden());

		UUID quizId = createQuizAsInstructor();
		mockMvc.perform(as(put("/api/v1/quizzes/" + quizId), STUDENT, "student")
						.contentType(MediaType.APPLICATION_JSON).content(payload()))
				.andExpect(status().isForbidden());
		mockMvc.perform(as(delete("/api/v1/quizzes/" + quizId), STUDENT, "student"))
				.andExpect(status().isForbidden());
		mockMvc.perform(as(post("/api/v1/quizzes/" + quizId + "/questions"), STUDENT, "student")
						.contentType(MediaType.APPLICATION_JSON).content(questionPayload()))
				.andExpect(status().isForbidden());
	}

	@Test
	void hocVienChiXemDuocDanhSachVaBanXemTruocKhongCoDapAn() throws Exception {
		UUID quizId = createQuizAsInstructor();

		mockMvc.perform(as(get("/api/v1/quizzes"), STUDENT, "student"))
				.andExpect(status().isOk());

		mockMvc.perform(as(get("/api/v1/quizzes/" + quizId + "/preview"), STUDENT, "student"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.questions[0].options[0].correct").doesNotExist());

		// Chi tiết kèm đáp án đúng chỉ dành cho giảng viên/admin.
		mockMvc.perform(as(get("/api/v1/quizzes/" + quizId), STUDENT, "student"))
				.andExpect(status().isForbidden());
	}

	@Test
	void giangVienSoHuuKhoaHocDuocCrudQuiz() throws Exception {
		UUID quizId = createQuizAsInstructor();

		mockMvc.perform(as(get("/api/v1/quizzes/" + quizId), INSTRUCTOR, "instructor"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.maxAttempts").value(3));

		mockMvc.perform(as(put("/api/v1/quizzes/" + quizId), INSTRUCTOR, "instructor")
						.contentType(MediaType.APPLICATION_JSON).content(payload()))
				.andExpect(status().isOk());

		mockMvc.perform(as(post("/api/v1/quizzes/" + quizId + "/questions"), INSTRUCTOR, "instructor")
						.contentType(MediaType.APPLICATION_JSON).content(questionPayload()))
				.andExpect(status().isCreated());

		mockMvc.perform(as(delete("/api/v1/quizzes/" + quizId), INSTRUCTOR, "instructor"))
				.andExpect(status().isNoContent());
	}

	@Test
	void giangVienKhacKhongSuaDuocQuizCuaNguoiKhac() throws Exception {
		UUID quizId = createQuizAsInstructor();

		mockMvc.perform(as(put("/api/v1/quizzes/" + quizId), OTHER_INSTRUCTOR, "instructor")
						.contentType(MediaType.APPLICATION_JSON).content(payload()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value(
						"Chỉ giảng viên phụ trách khoá học mới được sửa quiz của khoá học này"));

		mockMvc.perform(as(delete("/api/v1/quizzes/" + quizId), OTHER_INSTRUCTOR, "instructor"))
				.andExpect(status().isForbidden());

		// Vẫn không xoá được câu hỏi thuộc quiz của giảng viên khác.
		UUID questionId = quizRepository.findByIdWithContext(quizId).orElseThrow()
				.getQuestions().get(0).getId();
		mockMvc.perform(as(delete("/api/v1/quizzes/" + quizId + "/questions/" + questionId),
						OTHER_INSTRUCTOR, "instructor"))
				.andExpect(status().isForbidden());

		// Tạo quiz cho bài học thuộc khoá học của giảng viên khác cũng bị chặn.
		mockMvc.perform(as(post("/api/v1/quizzes"), INSTRUCTOR, "instructor")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payloadFor(spareQuizLesson.getId())))
				.andExpect(status().isForbidden());
	}

	@Test
	void adminDuocQuanLyQuizCuaMoiKhoaHoc() throws Exception {
		UUID quizId = createQuizAsInstructor();

		mockMvc.perform(as(get("/api/v1/quizzes/" + quizId), ADMIN, "admin"))
				.andExpect(status().isOk());
		mockMvc.perform(as(put("/api/v1/quizzes/" + quizId), ADMIN, "admin")
						.contentType(MediaType.APPLICATION_JSON).content(payload()))
				.andExpect(status().isOk());
		mockMvc.perform(as(delete("/api/v1/quizzes/" + quizId), ADMIN, "admin"))
				.andExpect(status().isNoContent());
	}

	// ------------------------------------------------------------- helpers

	private UUID createQuizAsInstructor() throws Exception {
		String body = mockMvc.perform(as(post("/api/v1/quizzes"), INSTRUCTOR, "instructor")
						.contentType(MediaType.APPLICATION_JSON).content(payload()))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return UUID.fromString(new tools.jackson.databind.ObjectMapper().readTree(body).path("id").asString());
	}

	private Lesson createQuizLesson(String instructorId, String lessonTitle) {
		Course course = courseRepository.save(Course.builder()
				.instructorId(UUID.fromString(instructorId))
				.title("Khoá học " + instructorId.substring(0, 4))
				.status(CourseStatus.DRAFT)
				.build());
		Section section = sectionRepository.save(Section.builder()
				.course(course).title("Chương 1").position(1).build());
		return lessonRepository.save(Lesson.builder()
				.section(section).title(lessonTitle).type(LessonType.QUIZ)
				.position(1).isPreview(false).build());
	}

	private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder builder, String subject, String role) {
		JwtRequestPostProcessor jwt = jwt()
				.jwt(j -> j.subject(subject).claim("realm_access", Map.of("roles", List.of(role))))
				.authorities(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
		return builder.with(jwt);
	}

	private String payload() {
		return payloadFor(quizLesson.getId());
	}

	private String payloadFor(UUID lessonId) {
		return """
				{"lessonId": "%s", "title": "Quiz phân quyền", "description": "test",
				 "passingScore": 70, "maxAttempts": 3,
				 "questions": [{"content": "1+1?", "type": "SINGLE_CHOICE",
				   "options": [{"content": "2", "correct": true}, {"content": "3", "correct": false}]}]}
				""".formatted(lessonId);
	}

	private String questionPayload() {
		return """
				{"content": "2+2?", "type": "SINGLE_CHOICE",
				 "options": [{"content": "4", "correct": true}, {"content": "5", "correct": false}]}
				""";
	}
}
