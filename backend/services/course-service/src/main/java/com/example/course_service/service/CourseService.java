package com.example.course_service.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.example.course_service.dto.CourseDetailResponse;
import com.example.course_service.dto.CourseRequest;
import com.example.course_service.dto.CourseSummaryResponse;
import com.example.course_service.dto.LessonRequest;
import com.example.course_service.dto.MaterialRequest;
import com.example.course_service.dto.PublishedCurriculumResponse;
import com.example.course_service.dto.SectionRequest;
import com.example.course_service.entity.CourseStatus;
import com.example.course_service.entity.MaterialType;

/**
 * Quản lý khoá học cho giảng viên: khoá học, chương, bài học, học liệu và vòng đời xuất bản.
 * Các thao tác ghi trên curriculum trả về chi tiết khoá học sau khi thay đổi.
 */
public interface CourseService {

	// ---------------------------------------------------------------- khoá học

	/** Tìm khoá học; mọi tham số đều tuỳ chọn (UC-C28). */
	List<CourseSummaryResponse> search(String keyword, CourseStatus status, Integer instructorId);

	/** Chi tiết khoá học kèm toàn bộ curriculum (UC-C29). */
	CourseDetailResponse getDetail(Integer id);

	/** Curriculum của khoá học đang PUBLISHED, dùng cho Progress Service (UC-P07). */
	PublishedCurriculumResponse getPublishedCurriculum(Integer id);

	CourseDetailResponse create(CourseRequest request);

	/** Sửa thông tin; {@code thumbnailUrl} để trống = giữ nguyên ảnh bìa hiện tại. */
	CourseDetailResponse update(Integer id, CourseRequest request);

	/** Upload ảnh bìa (image/*, tối đa 5 MB) lên MinIO, thay ảnh cũ nếu có. */
	CourseDetailResponse uploadThumbnail(Integer id, MultipartFile file);

	/** Bỏ ảnh bìa; ảnh trên MinIO bị xoá theo. */
	CourseDetailResponse removeThumbnail(Integer id);

	/** Xuất bản / ẩn / lưu trữ / khôi phục (UC-C21 → UC-C24). */
	CourseDetailResponse changeStatus(Integer id, CourseStatus target);

	void delete(Integer id);

	// ------------------------------------------------------------------ chương

	CourseDetailResponse addSection(Integer courseId, SectionRequest request);

	CourseDetailResponse updateSection(Integer sectionId, SectionRequest request);

	CourseDetailResponse deleteSection(Integer sectionId);

	/** Sắp xếp lại chương (UC-C10); {@code orderedIds} phải gồm đúng và đủ id hiện có. */
	CourseDetailResponse reorderSections(Integer courseId, List<Integer> orderedIds);

	// ----------------------------------------------------------------- bài học

	CourseDetailResponse addLesson(Integer sectionId, LessonRequest request);

	CourseDetailResponse updateLesson(Integer lessonId, LessonRequest request);

	CourseDetailResponse deleteLesson(Integer lessonId);

	CourseDetailResponse reorderLessons(Integer sectionId, List<Integer> orderedIds);

	// ----------------------------------------------------------------- học liệu

	/** Thêm học liệu dạng link ngoài (URL bắt buộc). */
	CourseDetailResponse addMaterial(Integer lessonId, MaterialRequest request);

	/**
	 * Upload file (video, tài liệu, hình ảnh) lên MinIO rồi gắn vào bài học (UC-C11).
	 * {@code name} / {@code type} có thể null: mặc định lấy tên file và đoán loại theo MIME type.
	 */
	CourseDetailResponse uploadMaterial(Integer lessonId, MultipartFile file, String name, MaterialType type);

	CourseDetailResponse updateMaterial(Integer materialId, MaterialRequest request);

	CourseDetailResponse deleteMaterial(Integer materialId);
}
