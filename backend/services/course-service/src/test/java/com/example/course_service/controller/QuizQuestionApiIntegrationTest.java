package com.example.course_service.controller;

import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.course_service.entity.Course;
import com.example.course_service.entity.CourseStatus;
import com.example.course_service.entity.Lesson;
import com.example.course_service.entity.LessonType;
import com.example.course_service.entity.Section;
import com.example.course_service.repository.CourseRepository;
import com.example.course_service.repository.LessonRepository;
import com.example.course_service.repository.SectionRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class QuizQuestionApiIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private CourseRepository courseRepository;

	@Autowired
	private SectionRepository sectionRepository;

	@Autowired
	private LessonRepository lessonRepository;

	private Lesson quizLesson;
	private Lesson emptyQuizLesson;

	@BeforeEach
	void setUp() {
		Course course = courseRepository.save(Course.builder()
				.instructorId(UUID.randomUUID()).title("Khoá học A").status(CourseStatus.PUBLISHED).build());
		Section section = sectionRepository.save(Section.builder()
				.course(course).title("Chương 1").position(1).build());
		quizLesson = lessonRepository.save(Lesson.builder()
				.section(section).title("Bài kiểm tra 1").type(LessonType.QUIZ)
				.position(1).isPreview(false).build());
		emptyQuizLesson = lessonRepository.save(Lesson.builder()
				.section(section).title("Bài kiểm tra 2").type(LessonType.QUIZ)
				.position(2).isPreview(false).build());

		Course other = courseRepository.save(Course.builder()
				.instructorId(UUID.randomUUID()).title("Khoá học B").status(CourseStatus.DRAFT).build());
		Section otherSection = sectionRepository.save(Section.builder()
				.course(other).title("Chương B1").position(1).build());
		lessonRepository.save(Lesson.builder()
				.section(otherSection).title("Bài kiểm tra B").type(LessonType.QUIZ)
				.position(1).isPreview(false).build());
	}

	// ------------------------------------------------------------- câu hỏi

	@Test
	void themCauHoiNoiTiepVaoCuoiQuiz() throws Exception {
		UUID quizId = createQuiz(quizLesson.getId(), "Quiz A");

		mockMvc.perform(post("/api/v1/quizzes/{id}/questions", quizId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(questionPayload("Câu hỏi thêm mới?", "MULTIPLE_CHOICE",
								option("A", true), option("B", true), option("C", false))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isString())
				.andExpect(jsonPath("$.position").value(3))
				.andExpect(jsonPath("$.options", hasSize(3)));

		mockMvc.perform(get("/api/v1/quizzes/{id}", quizId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.questions", hasSize(3)))
				.andExpect(jsonPath("$.questions[2].content").value("Câu hỏi thêm mới?"));
	}

	@Test
	void capNhatMotCauHoiKhongAnhHuongCauKhac() throws Exception {
		UUID quizId = createQuiz(quizLesson.getId(), "Quiz A");
		UUID questionId = questionIdOf(quizId, 0);

		mockMvc.perform(put("/api/v1/quizzes/{qid}/questions/{id}", quizId, questionId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(questionPayload("Nội dung đã sửa", "TRUE_FALSE",
								option("Đúng", false), option("Sai", true))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(questionId.toString()))
				.andExpect(jsonPath("$.content").value("Nội dung đã sửa"))
				.andExpect(jsonPath("$.type").value("TRUE_FALSE"))
				.andExpect(jsonPath("$.options", hasSize(2)))
				.andExpect(jsonPath("$.options[1].correct").value(true));

		mockMvc.perform(get("/api/v1/quizzes/{id}", quizId))
				.andExpect(jsonPath("$.questions", hasSize(2)))
				.andExpect(jsonPath("$.questions[0].id").value(questionId.toString()))
				.andExpect(jsonPath("$.questions[1].content").value("Câu hỏi gốc 2?"));
	}

	@Test
	void xoaCauHoiVaDanhLaiThuTu() throws Exception {
		UUID quizId = createQuiz(quizLesson.getId(), "Quiz A");
		UUID firstQuestionId = questionIdOf(quizId, 0);

		mockMvc.perform(delete("/api/v1/quizzes/{qid}/questions/{id}", quizId, firstQuestionId))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/quizzes/{id}", quizId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.questions", hasSize(1)))
				.andExpect(jsonPath("$.questions[0].content").value("Câu hỏi gốc 2?"))
				.andExpect(jsonPath("$.questions[0].position").value(1));
	}

	@Test
	void khongChoXoaCauHoiCuoiCung() throws Exception {
		UUID quizId = createQuiz(quizLesson.getId(), "Quiz A");
		UUID secondQuestionId = questionIdOf(quizId, 1);
		UUID firstQuestionId = questionIdOf(quizId, 0);
		mockMvc.perform(delete("/api/v1/quizzes/{qid}/questions/{id}", quizId, firstQuestionId))
				.andExpect(status().isNoContent());

		mockMvc.perform(delete("/api/v1/quizzes/{qid}/questions/{id}", quizId, secondQuestionId))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(
						"Quiz phải có ít nhất 1 câu hỏi. Hãy thêm câu hỏi khác trước khi xoá."));
	}

	@Test
	void sapXepLaiCauHoi() throws Exception {
		UUID quizId = createQuiz(quizLesson.getId(), "Quiz A");
		UUID first = questionIdOf(quizId, 0);
		UUID second = questionIdOf(quizId, 1);

		mockMvc.perform(put("/api/v1/quizzes/{id}/questions/order", quizId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"orderedIds\": [\"%s\", \"%s\"]}".formatted(second, first)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].id", contains(second.toString(), first.toString())))
				.andExpect(jsonPath("$[0].position").value(1))
				.andExpect(jsonPath("$[1].position").value(2));

		mockMvc.perform(get("/api/v1/quizzes/{id}", quizId))
				.andExpect(jsonPath("$.questions[0].id").value(second.toString()));
	}

	@Test
	void tuChoiSapXepThieuId() throws Exception {
		UUID quizId = createQuiz(quizLesson.getId(), "Quiz A");
		UUID first = questionIdOf(quizId, 0);

		mockMvc.perform(put("/api/v1/quizzes/{id}/questions/order", quizId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"orderedIds\": [\"%s\"]}".formatted(first)))
				.andExpect(status().isBadRequest());
	}

	// ------------------------------------------------------------ nhân bản

	@Test
	void nhanBanQuizSangBaiHocKhac() throws Exception {
		UUID quizId = createQuiz(quizLesson.getId(), "Quiz gốc");

		mockMvc.perform(post("/api/v1/quizzes/{id}/duplicate", quizId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"lessonId\": \"%s\"}".formatted(emptyQuizLesson.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isString())
				.andExpect(jsonPath("$.lessonId").value(emptyQuizLesson.getId().toString()))
				.andExpect(jsonPath("$.title").value("Quiz gốc"))
				.andExpect(jsonPath("$.questions", hasSize(2)))
				.andExpect(jsonPath("$.questions[0].options", hasSize(3)));

		mockMvc.perform(get("/api/v1/quizzes").param("lessonId", String.valueOf(emptyQuizLesson.getId())))
				.andExpect(jsonPath("$", hasSize(1)));
	}

	@Test
	void tuChoiNhanBanVaoBaiHocDaCoQuiz() throws Exception {
		UUID quizId = createQuiz(quizLesson.getId(), "Quiz gốc");

		mockMvc.perform(post("/api/v1/quizzes/{id}/duplicate", quizId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"lessonId\": \"%s\"}".formatted(quizLesson.getId())))
				.andExpect(status().isConflict());
	}

	// ----------------------------------------------------------- xem trước

	@Test
	void banXemTruocKhongLoDapAnDung() throws Exception {
		UUID quizId = createQuiz(quizLesson.getId(), "Quiz A");

		mockMvc.perform(get("/api/v1/quizzes/{id}/preview", quizId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Quiz A"))
				.andExpect(jsonPath("$.questions", hasSize(2)))
				.andExpect(jsonPath("$.questions[0].options", hasSize(3)))
				.andExpect(jsonPath("$.questions[0].options[0].correct").doesNotExist())
				.andExpect(jsonPath("$.questions[0].options[0].content").value("Đáp án 1"));
	}

	// -------------------------------------------------------------- lọc

	@Test
	void locTheoKhoaHocVaTuKhoa() throws Exception {
		UUID quizA = createQuiz(quizLesson.getId(), "Trắc nghiệm Java");
		createQuiz(lessonOfOtherCourse(), "Trắc nghiệm Python");

		mockMvc.perform(get("/api/v1/quizzes").param("keyword", "java"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(quizA.toString()));

		mockMvc.perform(get("/api/v1/quizzes").param("courseId", String.valueOf(
						lessonRepository.findById(quizLesson.getId()).orElseThrow().getSection().getCourse().getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)));

		mockMvc.perform(get("/api/v1/quizzes").param("keyword", "không-tồn-tại"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void danhSachTraVeThongTinSection() throws Exception {
		createQuiz(quizLesson.getId(), "Quiz A");

		mockMvc.perform(get("/api/v1/quizzes"))
				.andExpect(jsonPath("$[0].sectionId").isString())
				.andExpect(jsonPath("$[0].sectionTitle").value("Chương 1"));
	}

	// --------------------------------------------------------- validate

	@Test
	void tuChoiCauHoiChiCoMotDapAn() throws Exception {
		UUID quizId = createQuiz(quizLesson.getId(), "Quiz A");

		mockMvc.perform(post("/api/v1/quizzes/{id}/questions", quizId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(questionPayload("Sai", "SINGLE_CHOICE", option("Chỉ một", true))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void tuChoiTrueFalseKhongDuHaiDapAn() throws Exception {
		UUID quizId = createQuiz(quizLesson.getId(), "Quiz A");

		mockMvc.perform(post("/api/v1/quizzes/{id}/questions", quizId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(questionPayload("Sai", "TRUE_FALSE",
								option("A", true), option("B", false), option("C", false))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(
						"Câu hỏi 3 loại TRUE_FALSE phải có đúng 2 đáp án"));
	}

	@Test
	void tuChoiDapAnTrungNoiDung() throws Exception {
		UUID quizId = createQuiz(quizLesson.getId(), "Quiz A");

		mockMvc.perform(post("/api/v1/quizzes/{id}/questions", quizId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(questionPayload("Trùng đáp án", "SINGLE_CHOICE",
								option("Giống nhau", true), option("  giống NHAU  ", false))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(
						"Câu hỏi 3 có đáp án bị trùng nội dung"));
	}

	@Test
	void traVe404KhiCauHoiKhongThuocQuiz() throws Exception {
		UUID quizId = createQuiz(quizLesson.getId(), "Quiz A");

		mockMvc.perform(put("/api/v1/quizzes/{qid}/questions/{id}", quizId, java.util.UUID.randomUUID())
						.contentType(MediaType.APPLICATION_JSON)
						.content(questionPayload("X", "SINGLE_CHOICE", option("A", true), option("B", false))))
				.andExpect(status().isNotFound());
	}

	// ------------------------------------------------------------- helpers

	private UUID lessonOfOtherCourse() {
		return lessonRepository.findAllWithContextByType(LessonType.QUIZ).stream()
				.filter(lesson -> lesson.getSection().getCourse().getTitle().equals("Khoá học B"))
				.map(Lesson::getId)
				.findFirst()
				.orElseThrow();
	}

	private UUID questionIdOf(UUID quizId, int index) throws Exception {
		String response = mockMvc.perform(get("/api/v1/quizzes/{id}", quizId))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		JsonNode questions = objectMapper.readTree(response).path("questions");
		return UUID.fromString(questions.get(index).path("id").asText());
	}

	private UUID createQuiz(UUID lessonId, String title) throws Exception {
		String response = mockMvc.perform(post("/api/v1/quizzes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "lessonId": "%s",
								  "title": "%s",
								  "passingScore": 70,
								  "questions": [
								    {
								      "content": "Câu hỏi gốc 1?",
								      "type": "SINGLE_CHOICE",
								      "options": [
								        {"content": "Đáp án 1", "correct": true},
								        {"content": "Đáp án 2", "correct": false},
								        {"content": "Đáp án 3", "correct": false}
								      ]
								    },
								    {
								      "content": "Câu hỏi gốc 2?",
								      "type": "TRUE_FALSE",
								      "options": [
								        {"content": "Đúng", "correct": true},
								        {"content": "Sai", "correct": false}
								      ]
								    }
								  ]
								}
								""".formatted(lessonId, title)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return UUID.fromString(objectMapper.readTree(response).path("id").asText());
	}

	private String questionPayload(String content, String type, String... options) {
		return """
				{
				  "content": "%s",
				  "type": "%s",
				  "options": [%s]
				}
				""".formatted(content, type, String.join(",", List.of(options).stream().map(String::trim).toList()));
	}

	private String option(String content, boolean correct) {
		return "{\"content\": \"%s\", \"correct\": %s}".formatted(content, correct);
	}
}
