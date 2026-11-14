package com.example.course_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.example.course_service.client.EnrollmentClient;
import com.example.course_service.entity.Course;
import com.example.course_service.entity.CourseStatus;
import com.example.course_service.entity.Lesson;
import com.example.course_service.entity.LessonType;
import com.example.course_service.entity.Material;
import com.example.course_service.entity.MaterialType;
import com.example.course_service.entity.Section;
import com.example.course_service.repository.CourseRepository;
import com.example.course_service.repository.LessonRepository;
import com.example.course_service.repository.MaterialRepository;
import com.example.course_service.repository.SectionRepository;

/**
 * UC-C30: ai được xem học liệu (link video / tài liệu) trong trang chi tiết khoá học.
 * Bài "Xem thử" ai cũng xem được; bài khác chỉ học viên đã đăng ký và giảng viên phụ trách.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ContentAccessIntegrationTest {

	private static final String INSTRUCTOR = "22222222-2222-2222-2222-222222222222";
	private static final String STUDENT = "11111111-1111-1111-1111-111111111111";
	private static final String ADMIN = "33333333-3333-3333-3333-333333333333";

	private static final String PREVIEW_URL = "https://example.com/video-xem-thu.mp4";
	private static final String LOCKED_URL = "https://example.com/video-tra-phi.mp4";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CourseRepository courseRepository;

	@Autowired
	private SectionRepository sectionRepository;

	@Autowired
	private LessonRepository lessonRepository;

	@Autowired
	private MaterialRepository materialRepository;

	/** Giả lập Enrollment Service (không gọi service thật trong test). */
	@MockitoBean
	private EnrollmentClient enrollmentClient;

	private UUID courseId;

	@BeforeEach
	void setUp() {
		Course course = courseRepository.save(Course.builder()
				.instructorId(UUID.fromString(INSTRUCTOR))
				.title("Khoá học có bài xem thử")
				.status(CourseStatus.PUBLISHED)
				.build());
		courseId = course.getId();
		Section section = sectionRepository.save(Section.builder()
				.course(course).title("Chương 1").position(1).build());

		// Bài 1: cho xem thử; Bài 2: phải đăng ký
		Lesson preview = lessonRepository.save(Lesson.builder()
				.section(section).title("Bài 1").type(LessonType.VIDEO).position(1).isPreview(true).build());
		Lesson paid = lessonRepository.save(Lesson.builder()
				.section(section).title("Bài 2").type(LessonType.VIDEO).position(2).isPreview(false).build());

		materialRepository.save(Material.builder()
				.lesson(preview).name("Video xem thử").type(MaterialType.VIDEO).url(PREVIEW_URL).position(1).build());
		materialRepository.saveAndFlush(Material.builder()
				.lesson(paid).name("Video bài 2").type(MaterialType.VIDEO).url(LOCKED_URL).position(1).build());
	}

	@Test
	void khachChiXemDuocBaiXemThu() throws Exception {
		mockMvc.perform(get("/api/v1/courses/" + courseId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sections[0].lessons[0].locked").value(false))
				.andExpect(jsonPath("$.sections[0].lessons[0].materials[0].url").value(PREVIEW_URL))
				// Bài bị khoá: vẫn thấy tên học liệu nhưng không có link
				.andExpect(jsonPath("$.sections[0].lessons[1].locked").value(true))
				.andExpect(jsonPath("$.sections[0].lessons[1].materials[0].name").value("Video bài 2"))
				.andExpect(jsonPath("$.sections[0].lessons[1].materials[0].url").doesNotExist());
	}

	@Test
	void hocVienChuaDangKyBiKhoaBaiTraPhi() throws Exception {
		when(enrollmentClient.hasAccess(any(), any())).thenReturn(false);

		mockMvc.perform(as(get("/api/v1/courses/" + courseId), STUDENT, "student"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sections[0].lessons[1].locked").value(true))
				.andExpect(jsonPath("$.sections[0].lessons[1].materials[0].url").doesNotExist());
	}

	@Test
	void hocVienDaDangKyXemDuocMoiBai() throws Exception {
		when(enrollmentClient.hasAccess(UUID.fromString(STUDENT), courseId)).thenReturn(true);

		mockMvc.perform(as(get("/api/v1/courses/" + courseId), STUDENT, "student"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sections[0].lessons[1].locked").value(false))
				.andExpect(jsonPath("$.sections[0].lessons[1].materials[0].url").value(LOCKED_URL));
	}

	@Test
	void giangVienPhuTrachXemDuocMoiBai() throws Exception {
		mockMvc.perform(as(get("/api/v1/courses/" + courseId), INSTRUCTOR, "instructor"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sections[0].lessons[1].locked").value(false))
				.andExpect(jsonPath("$.sections[0].lessons[1].materials[0].url").value(LOCKED_URL));
	}

	@Test
	void adminCungKhongXemDuocBaiTraPhi() throws Exception {
		// Admin chỉ quản lý hệ thống, không học khoá học
		mockMvc.perform(as(get("/api/v1/courses/" + courseId), ADMIN, "admin"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sections[0].lessons[1].locked").value(true))
				.andExpect(jsonPath("$.sections[0].lessons[1].materials[0].url").doesNotExist());
	}

	private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder builder, String subject, String role) {
		return builder.with(jwt()
				.jwt(j -> j.subject(subject).claim("realm_access", Map.of("roles", List.of(role))))
				.authorities(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())));
	}
}
