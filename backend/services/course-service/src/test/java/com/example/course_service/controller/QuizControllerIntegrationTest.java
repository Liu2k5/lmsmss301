package com.example.course_service.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.example.course_service.repository.QuizRepository;
import com.example.course_service.repository.SectionRepository;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class QuizControllerIntegrationTest {

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

	@Autowired
	private QuizRepository quizRepository;

	private Lesson quizLesson;
	private Lesson secondQuizLesson;
	private Lesson videoLesson;

	@BeforeEach
	void setUp() {
		Course course = courseRepository.save(Course.builder()
				.instructorId(1)
				.title("Khoá học test")
				.status(CourseStatus.PUBLISHED)
				.build());
		Section section = sectionRepository.save(Section.builder()
				.course(course)
				.title("Chương 1")
				.position(1)
				.build());
		quizLesson = lessonRepository.save(Lesson.builder()
				.section(section).title("Bài kiểm tra 1").type(LessonType.QUIZ)
				.position(1).isPreview(false).build());
		secondQuizLesson = lessonRepository.save(Lesson.builder()
				.section(section).title("Bài kiểm tra 2").type(LessonType.QUIZ)
				.position(2).isPreview(false).build());
		videoLesson = lessonRepository.save(Lesson.builder()
				.section(section).title("Bài video").type(LessonType.VIDEO)
				.position(3).isPreview(true).build());
	}

	@Test
	void taoQuizTraVe201VaDayDuCauHoiDapAn() throws Exception {
		mockMvc.perform(post("/api/v1/quizzes")
						.contentType(MediaType.APPLICATION_JSON)
						.content(validPayload(quizLesson.getId(), "Quiz mới")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.lessonId").value(quizLesson.getId()))
				.andExpect(jsonPath("$.lessonTitle").value("Bài kiểm tra 1"))
				.andExpect(jsonPath("$.courseTitle").value("Khoá học test"))
				.andExpect(jsonPath("$.title").value("Quiz mới"))
				.andExpect(jsonPath("$.passingScore").value(70.0))
				.andExpect(jsonPath("$.questions", hasSize(2)))
				.andExpect(jsonPath("$.questions[0].position").value(1))
				.andExpect(jsonPath("$.questions[0].options", hasSize(3)))
				.andExpect(jsonPath("$.questions[0].options[0].correct").value(true))
				.andExpect(jsonPath("$.questions[0].options[0].position").value(1))
				.andExpect(jsonPath("$.questions[1].options[1].correct").value(true));
	}

	@Test
	void layDanhSachTraVeTomTatKemSoCauHoi() throws Exception {
		createQuiz(quizLesson.getId(), "Quiz A");
		createQuiz(secondQuizLesson.getId(), "Quiz B");

		mockMvc.perform(get("/api/v1/quizzes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].questionCount").value(2))
				.andExpect(jsonPath("$[0].lessonTitle").isString());

		mockMvc.perform(get("/api/v1/quizzes").param("lessonId", String.valueOf(quizLesson.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].lessonId").value(quizLesson.getId()));
	}

	@Test
	void layChiTietTheoId() throws Exception {
		Integer id = createQuiz(quizLesson.getId(), "Quiz chi tiết");

		mockMvc.perform(get("/api/v1/quizzes/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.description").value("Mô tả quiz"))
				.andExpect(jsonPath("$.questions[0].type").value("SINGLE_CHOICE"));
	}

	@Test
	void capNhatThayDoiCauHoiVaXoaCauHoiKhongConTrongPayload() throws Exception {
		Integer id = createQuiz(quizLesson.getId(), "Quiz cũ");

		String updatePayload = """
				{
				  "lessonId": %d,
				  "title": "Quiz đã sửa",
				  "description": "Mô tả mới",
				  "passingScore": 85.5,
				  "questions": [
				    {
				      "content": "Câu hỏi duy nhất còn lại?",
				      "type": "MULTIPLE_CHOICE",
				      "options": [
				        {"content": "Đáp án A", "correct": true},
				        {"content": "Đáp án B", "correct": true},
				        {"content": "Đáp án C", "correct": false}
				      ]
				    }
				  ]
				}
				""".formatted(quizLesson.getId());

		mockMvc.perform(put("/api/v1/quizzes/{id}", id)
						.contentType(MediaType.APPLICATION_JSON)
						.content(updatePayload))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Quiz đã sửa"))
				.andExpect(jsonPath("$.passingScore").value(85.5))
				.andExpect(jsonPath("$.questions", hasSize(1)))
				.andExpect(jsonPath("$.questions[0].type").value("MULTIPLE_CHOICE"))
				.andExpect(jsonPath("$.questions[0].options", hasSize(3)));

		mockMvc.perform(get("/api/v1/quizzes/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.questions", hasSize(1)))
				.andExpect(jsonPath("$.description").value("Mô tả mới"));
	}

	@Test
	void giuNguyenIdCuaCauHoiVaDapAnKhiCapNhat() throws Exception {
		Integer id = createQuiz(quizLesson.getId(), "Quiz giữ id");
		String current = mockMvc.perform(get("/api/v1/quizzes/{id}", id))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		int questionId = objectMapper.readTree(current).path("questions").get(0).path("id").asInt();
		int optionId = objectMapper.readTree(current).path("questions").get(0)
				.path("options").get(0).path("id").asInt();

		String payload = """
				{
				  "lessonId": %d,
				  "title": "Quiz giữ id",
				  "passingScore": 50,
				  "questions": [
				    {
				      "id": %d,
				      "content": "Nội dung đã sửa",
				      "type": "SINGLE_CHOICE",
				      "options": [
				        {"id": %d, "content": "Đáp án đã sửa", "correct": true},
				        {"content": "Đáp án mới", "correct": false}
				      ]
				    }
				  ]
				}
				""".formatted(quizLesson.getId(), questionId, optionId);

		mockMvc.perform(put("/api/v1/quizzes/{id}", id)
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.questions[0].id").value(questionId))
				.andExpect(jsonPath("$.questions[0].content").value("Nội dung đã sửa"))
				.andExpect(jsonPath("$.questions[0].options[0].id").value(optionId))
				.andExpect(jsonPath("$.questions[0].options[0].content").value("Đáp án đã sửa"));
	}

	@Test
	void xoaQuizTraVe204VaKhongConTimThay() throws Exception {
		Integer id = createQuiz(quizLesson.getId(), "Quiz sẽ xoá");

		mockMvc.perform(delete("/api/v1/quizzes/{id}", id))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/quizzes/{id}", id))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Không tìm thấy quiz id=" + id));
	}

	@Test
	void xoaQuizXoaLuonCauHoiVaDapAn() throws Exception {
		Integer id = createQuiz(quizLesson.getId(), "Quiz cascade");
		mockMvc.perform(delete("/api/v1/quizzes/{id}", id)).andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/quizzes")).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void khongChoTaoHaiQuizTrenCungMotBaiHoc() throws Exception {
		createQuiz(quizLesson.getId(), "Quiz 1");

		mockMvc.perform(post("/api/v1/quizzes")
						.contentType(MediaType.APPLICATION_JSON)
						.content(validPayload(quizLesson.getId(), "Quiz 2")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Bài học này đã có quiz. Mỗi bài học chỉ có một quiz."));
	}

	@Test
	void tuChoiTaoQuizTrenBaiHocKhongPhaiLoaiQuiz() throws Exception {
		mockMvc.perform(post("/api/v1/quizzes")
						.contentType(MediaType.APPLICATION_JSON)
						.content(validPayload(videoLesson.getId(), "Quiz sai loại")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Chỉ bài học loại QUIZ mới có thể gắn quiz"));
	}

	@Test
	void tuChoiCauHoiKhongCoDapAnDung() throws Exception {
		String payload = """
				{
				  "lessonId": %d,
				  "title": "Quiz sai",
				  "passingScore": 50,
				  "questions": [
				    {
				      "content": "Không có đáp án đúng?",
				      "type": "SINGLE_CHOICE",
				      "options": [
				        {"content": "A", "correct": false},
				        {"content": "B", "correct": false}
				      ]
				    }
				  ]
				}
				""".formatted(quizLesson.getId());

		mockMvc.perform(post("/api/v1/quizzes")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Câu hỏi 1 phải có ít nhất 1 đáp án đúng"));
	}

	@Test
	void tuChoiSingleChoiceCoNhieuHonMotDapAnDung() throws Exception {
		String payload = """
				{
				  "lessonId": %d,
				  "title": "Quiz sai",
				  "passingScore": 50,
				  "questions": [
				    {
				      "content": "Chọn một?",
				      "type": "SINGLE_CHOICE",
				      "options": [
				        {"content": "A", "correct": true},
				        {"content": "B", "correct": true}
				      ]
				    }
				  ]
				}
				""".formatted(quizLesson.getId());

		mockMvc.perform(post("/api/v1/quizzes")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
						.value("Câu hỏi 1 loại SINGLE_CHOICE phải có đúng 1 đáp án đúng"));
	}

	@Test
	void baoLoiValidateKemChiTietField() throws Exception {
		String payload = """
				{
				  "lessonId": %d,
				  "title": "  ",
				  "passingScore": 150,
				  "questions": []
				}
				""".formatted(quizLesson.getId());

		mockMvc.perform(post("/api/v1/quizzes")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Dữ liệu không hợp lệ"))
				.andExpect(jsonPath("$.details", hasSize(3)));
	}

	@Test
	void traVeDanhSachBaiHocQuizKemQuizHienCo() throws Exception {
		createQuiz(quizLesson.getId(), "Quiz đã có");

		mockMvc.perform(get("/api/v1/quizzes/lesson-options"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[?(@.id == %d)].quizId".formatted(quizLesson.getId()))
						.value(org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.notNullValue())))
				.andExpect(jsonPath("$[?(@.id == %d)].quizId".formatted(secondQuizLesson.getId()))
						.value(org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.nullValue())));
	}

	@Test
	void traVe404KhiQuizKhongTonTai() throws Exception {
		mockMvc.perform(get("/api/v1/quizzes/{id}", 999999))
				.andExpect(status().isNotFound());
	}

	private Integer createQuiz(Integer lessonId, String title) throws Exception {
		String response = mockMvc.perform(post("/api/v1/quizzes")
						.contentType(MediaType.APPLICATION_JSON)
						.content(validPayload(lessonId, title)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(response).path("id").asInt();
	}

	private String validPayload(Integer lessonId, String title) {
		return """
				{
				  "lessonId": %d,
				  "title": "%s",
				  "description": "Mô tả quiz",
				  "passingScore": 70.00,
				  "questions": [
				    {
				      "content": "Câu hỏi 1?",
				      "type": "SINGLE_CHOICE",
				      "options": [
				        {"content": "Đáp án đúng", "correct": true},
				        {"content": "Đáp án sai 1", "correct": false},
				        {"content": "Đáp án sai 2", "correct": false}
				      ]
				    },
				    {
				      "content": "Câu hỏi 2?",
				      "type": "TRUE_FALSE",
				      "options": [
				        {"content": "Đúng", "correct": false},
				        {"content": "Sai", "correct": true}
				      ]
				    }
				  ]
				}
				""".formatted(lessonId, title);
	}
}
