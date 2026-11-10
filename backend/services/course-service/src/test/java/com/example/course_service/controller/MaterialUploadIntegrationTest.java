package com.example.course_service.controller;

import com.example.course_service.support.TestLogin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import com.example.course_service.entity.Course;
import com.example.course_service.entity.CourseStatus;
import com.example.course_service.entity.Lesson;
import com.example.course_service.entity.LessonType;
import com.example.course_service.entity.Section;
import com.example.course_service.exception.StorageException;
import com.example.course_service.repository.CourseRepository;
import com.example.course_service.repository.LessonRepository;
import com.example.course_service.repository.MaterialRepository;
import com.example.course_service.repository.SectionRepository;
import com.example.course_service.service.StorageService;

import tools.jackson.databind.ObjectMapper;

/**
 * Upload học liệu lên MinIO. MinIO được mock nên test không cần MinIO thật.
 * Không dùng @Transactional vì việc xoá file chạy sau khi transaction commit.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) // test nghiệp vụ: bỏ qua xác thực, phần quyền có test riêng
class MaterialUploadIntegrationTest {

	/** Test nghiệp vụ tắt filter bảo mật → tự đăng nhập bằng giảng viên chủ các khoá học trong test. */
	@BeforeEach
	void loginAsInstructor() {
		TestLogin.asInstructor();
	}

	@AfterEach
	void logout() {
		TestLogin.logout();
	}

	private static final String OBJECT_KEY = "courses/1/lessons/1/abc-bai-giang.mp4";

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
	private MaterialRepository materialRepository;

	@MockitoBean
	private StorageService storageService;

	private Lesson lesson;

	@BeforeEach
	void setUp() {
		Course course = courseRepository.save(Course.builder()
				.instructorId(java.util.UUID.fromString(TestLogin.INSTRUCTOR_ID)).title("Khoá upload").status(CourseStatus.DRAFT).build());
		Section section = sectionRepository.save(Section.builder()
				.course(course).title("Chương 1").position(1).build());
		lesson = lessonRepository.save(Lesson.builder()
				.section(section).title("Bài video").type(LessonType.VIDEO).position(1).isPreview(false).build());

		when(storageService.upload(anyString(), any()))
				.thenReturn(new StorageService.StoredFile(OBJECT_KEY, 11, "video/mp4"));
		when(storageService.viewUrl(anyString()))
				.thenAnswer(invocation -> "http://minio.local/presigned/" + invocation.getArgument(0));
	}

	@AfterEach
	void cleanUp() {
		materialRepository.deleteAll();
		lessonRepository.deleteAll();
		sectionRepository.deleteAll();
		courseRepository.deleteAll();
	}

	@Test
	void uploadVideoLuuKhoaMinioVaTraVeLinkXem() throws Exception {
		mockMvc.perform(multipart("/api/v1/lessons/{id}/materials/upload", lesson.getId())
						.file(videoFile()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.sections[0].lessons[0].materials", hasSize(1)))
				.andExpect(jsonPath("$.sections[0].lessons[0].materials[0].name").value("bai-giang.mp4"))
				.andExpect(jsonPath("$.sections[0].lessons[0].materials[0].type").value("VIDEO"))
				.andExpect(jsonPath("$.sections[0].lessons[0].materials[0].uploaded").value(true))
				.andExpect(jsonPath("$.sections[0].lessons[0].materials[0].fileSize").value(11))
				.andExpect(jsonPath("$.sections[0].lessons[0].materials[0].url")
						.value("http://minio.local/presigned/" + OBJECT_KEY));

		verify(storageService).upload(startsWith("courses/"), any());
		// DB lưu khoá object, không lưu link tạm.
		assertThat(materialRepository.findAll().get(0).getUrl()).isEqualTo("minio:" + OBJECT_KEY);
	}

	@Test
	void uploadCoTenVaLoaiTuChon() throws Exception {
		mockMvc.perform(multipart("/api/v1/lessons/{id}/materials/upload", lesson.getId())
						.file(videoFile())
						.param("name", "Video bài 1")
						.param("type", "OTHER"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.sections[0].lessons[0].materials[0].name").value("Video bài 1"))
				.andExpect(jsonPath("$.sections[0].lessons[0].materials[0].type").value("OTHER"));
	}

	@Test
	void khoaHocDaLuuTruThiKhongUploadLenMinio() throws Exception {
		Course course = courseRepository.findById(lesson.getSection().getCourse().getId()).orElseThrow();
		course.setStatus(CourseStatus.ARCHIVED);
		courseRepository.save(course);

		mockMvc.perform(multipart("/api/v1/lessons/{id}/materials/upload", lesson.getId()).file(videoFile()))
				.andExpect(status().isBadRequest());
		mockMvc.perform(multipart("/api/v1/courses/{id}/thumbnail", course.getId()).file(imageFile()))
				.andExpect(status().isBadRequest());
		// Kiểm tra trước khi upload nên không tốn công đẩy file lên MinIO.
		verify(storageService, never()).upload(anyString(), any());
	}

	@Test
	void baiHocBiXoaTrongLucUploadThiXoaFileVuaUpload() throws Exception {
		// Giả lập: trong lúc file đang được đẩy lên MinIO thì bài học bị xoá.
		when(storageService.upload(anyString(), any())).thenAnswer(invocation -> {
			lessonRepository.deleteById(lesson.getId());
			return new StorageService.StoredFile(OBJECT_KEY, 11, "video/mp4");
		});

		mockMvc.perform(multipart("/api/v1/lessons/{id}/materials/upload", lesson.getId()).file(videoFile()))
				.andExpect(status().isNotFound());
		verify(storageService).delete(OBJECT_KEY);
		assertThat(materialRepository.count()).isZero();
	}

	@Test
	void uploadFileRongTraVe400() throws Exception {
		mockMvc.perform(multipart("/api/v1/lessons/{id}/materials/upload", lesson.getId())
						.file(new MockMultipartFile("file", "rong.mp4", "video/mp4", new byte[0])))
				.andExpect(status().isBadRequest());
		verify(storageService, never()).upload(anyString(), any());
	}

	@Test
	void minioLoiTraVe503VaKhongLuuHocLieu() throws Exception {
		when(storageService.upload(anyString(), any()))
				.thenThrow(new StorageException("Không kết nối được MinIO", new RuntimeException()));

		mockMvc.perform(multipart("/api/v1/lessons/{id}/materials/upload", lesson.getId())
						.file(videoFile()))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.message").value("Không kết nối được MinIO"));
		assertThat(materialRepository.count()).isZero();
	}

	@Test
	void suaHocLieuDaUploadKhongGuiUrlThiGiuNguyenFile() throws Exception {
		String materialId = uploadAndGetMaterialId();

		mockMvc.perform(put("/api/v1/materials/{id}", materialId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\": \"Tên mới\", \"type\": \"VIDEO\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sections[0].lessons[0].materials[0].name").value("Tên mới"))
				.andExpect(jsonPath("$.sections[0].lessons[0].materials[0].uploaded").value(true));

		verify(storageService, never()).delete(anyString());
	}

	@Test
	void xoaHocLieuXoaLuonFileTrenMinio() throws Exception {
		String materialId = uploadAndGetMaterialId();

		mockMvc.perform(delete("/api/v1/materials/{id}", materialId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sections[0].lessons[0].materials", hasSize(0)));
		verify(storageService).delete(OBJECT_KEY);
	}

	@Test
	void xoaBaiHocXoaLuonFileTrenMinio() throws Exception {
		uploadAndGetMaterialId();

		mockMvc.perform(delete("/api/v1/lessons/{id}", lesson.getId())).andExpect(status().isOk());
		verify(storageService).delete(OBJECT_KEY);
	}

	@Test
	void themLinkKhongCoUrlTraVe400() throws Exception {
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
						.post("/api/v1/lessons/{id}/materials", lesson.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\": \"Slide\", \"type\": \"DOCUMENT\"}"))
				.andExpect(status().isBadRequest());
	}

	// --------------------------------------------------------------- ảnh bìa

	@Test
	void uploadAnhBiaLuuMinioVaTraVeLinkXem() throws Exception {
		UUID courseId = lesson.getSection().getCourse().getId();
		when(storageService.upload(anyString(), any()))
				.thenReturn(new StorageService.StoredFile("courses/1/thumbnail/a-bia.png", 4, "image/png"));

		mockMvc.perform(multipart("/api/v1/courses/{id}/thumbnail", courseId).file(imageFile()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.thumbnailUrl").value("http://minio.local/presigned/courses/1/thumbnail/a-bia.png"));

		verify(storageService).upload(startsWith("courses/" + courseId + "/thumbnail"), any());
		assertThat(courseRepository.findById(courseId).orElseThrow().getThumbnailUrl())
				.isEqualTo("minio:courses/1/thumbnail/a-bia.png");

		// Danh sách khoá học cũng trả link xem được, không trả khoá minio:
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/instructor/courses"))
				.andExpect(jsonPath("$[0].thumbnailUrl").value("http://minio.local/presigned/courses/1/thumbnail/a-bia.png"));
	}

	@Test
	void doiAnhBiaXoaAnhCuTrenMinio() throws Exception {
		UUID courseId = lesson.getSection().getCourse().getId();
		when(storageService.upload(anyString(), any()))
				.thenReturn(new StorageService.StoredFile("cu.png", 4, "image/png"))
				.thenReturn(new StorageService.StoredFile("moi.png", 4, "image/png"));

		mockMvc.perform(multipart("/api/v1/courses/{id}/thumbnail", courseId).file(imageFile()))
				.andExpect(status().isOk());
		mockMvc.perform(multipart("/api/v1/courses/{id}/thumbnail", courseId).file(imageFile()))
				.andExpect(status().isOk());

		verify(storageService).delete("cu.png");
		verify(storageService, never()).delete("moi.png");
	}

	@Test
	void anhBiaKhongPhaiHinhAnhTraVe400() throws Exception {
		UUID courseId = lesson.getSection().getCourse().getId();
		mockMvc.perform(multipart("/api/v1/courses/{id}/thumbnail", courseId).file(videoFile()))
				.andExpect(status().isBadRequest());
		verify(storageService, never()).upload(anyString(), any());
	}

	@Test
	void suaKhoaHocKhongGuiAnhBiaThiGiuAnhCuVaBoAnhBiaXoaFile() throws Exception {
		UUID courseId = lesson.getSection().getCourse().getId();
		when(storageService.upload(anyString(), any()))
				.thenReturn(new StorageService.StoredFile("bia.png", 4, "image/png"));
		mockMvc.perform(multipart("/api/v1/courses/{id}/thumbnail", courseId).file(imageFile()))
				.andExpect(status().isOk());

		mockMvc.perform(put("/api/v1/courses/{id}", courseId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"instructorId\": \"22222222-2222-2222-2222-222222222222\", \"title\": \"Tên mới\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.thumbnailUrl").value("http://minio.local/presigned/bia.png"));

		mockMvc.perform(delete("/api/v1/courses/{id}/thumbnail", courseId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.thumbnailUrl").doesNotExist());
		verify(storageService).delete("bia.png");
	}

	private MockMultipartFile imageFile() {
		return new MockMultipartFile("file", "bia.png", "image/png", new byte[] { 1, 2, 3, 4 });
	}

	private MockMultipartFile videoFile() {
		return new MockMultipartFile("file", "bai-giang.mp4", "video/mp4", "fake-video!".getBytes());
	}

	private String uploadAndGetMaterialId() throws Exception {
		String body = mockMvc.perform(multipart("/api/v1/lessons/{id}/materials/upload", lesson.getId())
						.file(videoFile()))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(body)
				.get("sections").get(0).get("lessons").get(0).get("materials").get(0).get("id").asText();
	}
}
