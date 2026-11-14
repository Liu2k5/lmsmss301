package com.example.course_service.controller;

import static org.hamcrest.Matchers.hasSize;
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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.example.course_service.entity.Category;
import com.example.course_service.entity.Course;
import com.example.course_service.entity.CourseStatus;
import com.example.course_service.repository.CategoryRepository;
import com.example.course_service.repository.CourseRepository;

/** CRUD danh mục khoá học: xem công khai, chỉ Admin được tạo / sửa / xoá. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CategoryControllerIntegrationTest {

	private static final String ADMIN = "33333333-3333-3333-3333-333333333333";
	private static final String INSTRUCTOR = "22222222-2222-2222-2222-222222222222";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private CourseRepository courseRepository;

	// ------------------------------------------------------------------ xem

	@Test
	void khachXemDuocDanhSachKemSoKhoaHoc() throws Exception {
		Category backend = saveCategory("Backend");
		saveCategory("Frontend");
		saveCourse(backend);
		saveCourse(backend);

		mockMvc.perform(get("/api/v1/categories"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				// Sắp theo tên A → Z
				.andExpect(jsonPath("$[0].name").value("Backend"))
				.andExpect(jsonPath("$[0].courseCount").value(2))
				.andExpect(jsonPath("$[1].name").value("Frontend"))
				.andExpect(jsonPath("$[1].courseCount").value(0));
	}

	@Test
	void xemChiTietKhongTonTaiTra404() throws Exception {
		mockMvc.perform(get("/api/v1/categories/" + UUID.randomUUID()))
				.andExpect(status().isNotFound());
	}

	// ------------------------------------------------------------------ tạo

	@Test
	void adminTaoDuocDanhMuc() throws Exception {
		mockMvc.perform(asAdmin(post("/api/v1/categories"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"  Khoa học dữ liệu  ","description":"Python, ML"}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.name").value("Khoa học dữ liệu"))
				.andExpect(jsonPath("$.courseCount").value(0));
	}

	@Test
	void trungTenKhongPhanBietHoaThuongTra409() throws Exception {
		saveCategory("Backend");

		mockMvc.perform(asAdmin(post("/api/v1/categories"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"BACKEND"}
						"""))
				.andExpect(status().isConflict());
	}

	@Test
	void tenTrongTra400() throws Exception {
		mockMvc.perform(asAdmin(post("/api/v1/categories"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"   "}
						"""))
				.andExpect(status().isBadRequest());
	}

	// ------------------------------------------------------------------ phân quyền

	@Test
	void chuaDangNhapKhongTaoDuoc() throws Exception {
		mockMvc.perform(post("/api/v1/categories")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"Backend"}
						"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void giangVienKhongTaoSuaXoaDuoc() throws Exception {
		Category category = saveCategory("Backend");

		mockMvc.perform(as(post("/api/v1/categories"), INSTRUCTOR, "instructor")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"Mới"}
						"""))
				.andExpect(status().isForbidden());
		mockMvc.perform(as(put("/api/v1/categories/" + category.getId()), INSTRUCTOR, "instructor")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"Đổi tên"}
						"""))
				.andExpect(status().isForbidden());
		mockMvc.perform(as(delete("/api/v1/categories/" + category.getId()), INSTRUCTOR, "instructor"))
				.andExpect(status().isForbidden());
	}

	// ------------------------------------------------------------------ sửa

	@Test
	void adminSuaDuocVaGiuNguyenTenCuaChinhNo() throws Exception {
		Category category = saveCategory("Backend");

		// Giữ nguyên tên (chỉ đổi hoa thường) + đổi mô tả: không bị coi là trùng với chính nó
		mockMvc.perform(asAdmin(put("/api/v1/categories/" + category.getId()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"backend","description":"Mô tả mới"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("backend"))
				.andExpect(jsonPath("$.description").value("Mô tả mới"));
	}

	@Test
	void suaTrungTenDanhMucKhacTra409() throws Exception {
		saveCategory("Backend");
		Category frontend = saveCategory("Frontend");

		mockMvc.perform(asAdmin(put("/api/v1/categories/" + frontend.getId()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"Backend"}
						"""))
				.andExpect(status().isConflict());
	}

	// ------------------------------------------------------------------ xoá

	@Test
	void adminXoaDuocDanhMucRong() throws Exception {
		Category category = saveCategory("Backend");

		mockMvc.perform(asAdmin(delete("/api/v1/categories/" + category.getId())))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/categories/" + category.getId()))
				.andExpect(status().isNotFound());
	}

	@Test
	void khongXoaDuocDanhMucConKhoaHoc() throws Exception {
		Category category = saveCategory("Backend");
		saveCourse(category);

		mockMvc.perform(asAdmin(delete("/api/v1/categories/" + category.getId())))
				.andExpect(status().isConflict());
	}

	// ------------------------------------------------------------------ nối với khoá học

	@Test
	void locKhoaHocTheoDanhMuc() throws Exception {
		Category backend = saveCategory("Backend");
		Category frontend = saveCategory("Frontend");
		Course backendCourse = saveCourse(backend);
		saveCourse(frontend);

		mockMvc.perform(get("/api/v1/courses").param("categoryId", backend.getId().toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(backendCourse.getId().toString()))
				.andExpect(jsonPath("$[0].categoryName").value("Backend"));
	}

	@Test
	void taoKhoaHocVoiDanhMucKhongTonTaiTra404() throws Exception {
		mockMvc.perform(as(post("/api/v1/courses"), INSTRUCTOR, "instructor")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"instructorId":"%s","title":"Khoá học mới","categoryId":"%s"}
						""".formatted(INSTRUCTOR, UUID.randomUUID())))
				.andExpect(status().isNotFound());
	}

	@Test
	void taoKhoaHocCoDanhMuc() throws Exception {
		Category backend = saveCategory("Backend");

		mockMvc.perform(as(post("/api/v1/courses"), INSTRUCTOR, "instructor")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"instructorId":"%s","title":"Khoá học mới","categoryId":"%s"}
						""".formatted(INSTRUCTOR, backend.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.categoryId").value(backend.getId().toString()))
				.andExpect(jsonPath("$.categoryName").value("Backend"));
	}

	// ------------------------------------------------------------------ hàm phụ

	private Category saveCategory(String name) {
		return categoryRepository.saveAndFlush(Category.builder().name(name).build());
	}

	private Course saveCourse(Category category) {
		return courseRepository.saveAndFlush(Course.builder()
				.instructorId(UUID.fromString(INSTRUCTOR))
				.title("Khoá học " + UUID.randomUUID())
				.status(CourseStatus.PUBLISHED)
				.category(category)
				.build());
	}

	private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder builder) {
		return as(builder, ADMIN, "admin");
	}

	private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder builder, String subject, String role) {
		return builder.with(jwt()
				.jwt(j -> j.subject(subject).claim("realm_access", Map.of("roles", List.of(role))))
				.authorities(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())));
	}
}
