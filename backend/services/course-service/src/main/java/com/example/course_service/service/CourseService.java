package com.example.course_service.service;

import java.util.List;
import java.util.UUID;

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
import com.example.course_service.security.LoginUser;

/**
 * Quản lý khoá học cho giảng viên: khoá học, chương, bài học, học liệu và vòng đời xuất bản.
 * Các thao tác ghi trên curriculum trả về chi tiết khoá học sau khi thay đổi.
 */
public interface CourseService {

	// ---------------------------------------------------------------- khoá học

	/** Tìm khoá học công khai (không có bản nháp); mọi tham số đều tuỳ chọn (UC-C28). */
	List<CourseSummaryResponse> search(String keyword, CourseStatus status, UUID instructorId, UUID categoryId);

	/** Chi tiết khoá học công khai kèm curriculum (UC-C29); khoá nháp → 404. */
	CourseDetailResponse getDetail(LoginUser user, UUID id);

	/** Khoá học của chính giảng viên đang đăng nhập, gồm cả bản nháp. */
	List<CourseSummaryResponse> searchMine(LoginUser user, String keyword, CourseStatus status, UUID categoryId);

	/** Chi tiết khoá học của chính giảng viên đang đăng nhập (mọi trạng thái); khoá của người khác → 404. */
	CourseDetailResponse getMyDetail(LoginUser user, UUID id);

	/** Curriculum của khoá học đang PUBLISHED, dùng cho Progress Service (UC-P07). */
	PublishedCurriculumResponse getPublishedCurriculum(UUID id);

	CourseDetailResponse create(LoginUser user, CourseRequest request);

	/** Sửa thông tin; {@code thumbnailUrl} để trống = giữ nguyên ảnh bìa hiện tại. */
	CourseDetailResponse update(LoginUser user, UUID id, CourseRequest request);

	/** Upload ảnh bìa (image/*, tối đa 5 MB) lên MinIO, thay ảnh cũ nếu có. */
	CourseDetailResponse uploadThumbnail(LoginUser user, UUID id, MultipartFile file);

	/** Bỏ ảnh bìa; ảnh trên MinIO bị xoá theo. */
	CourseDetailResponse removeThumbnail(LoginUser user, UUID id);

	/** Xuất bản / ẩn / lưu trữ / khôi phục (UC-C21 → UC-C24). */
	CourseDetailResponse changeStatus(LoginUser user, UUID id, CourseStatus target);

	void delete(LoginUser user, UUID id);

	// ------------------------------------------------------------------ chương

	CourseDetailResponse addSection(LoginUser user, UUID courseId, SectionRequest request);

	CourseDetailResponse updateSection(LoginUser user, UUID sectionId, SectionRequest request);

	CourseDetailResponse deleteSection(LoginUser user, UUID sectionId);

	/** Sắp xếp lại chương (UC-C10); {@code orderedIds} phải gồm đúng và đủ id hiện có. */
	CourseDetailResponse reorderSections(LoginUser user, UUID courseId, List<UUID> orderedIds);

	// ----------------------------------------------------------------- bài học

	CourseDetailResponse addLesson(LoginUser user, UUID sectionId, LessonRequest request);

	CourseDetailResponse updateLesson(LoginUser user, UUID lessonId, LessonRequest request);

	CourseDetailResponse deleteLesson(LoginUser user, UUID lessonId);

	CourseDetailResponse reorderLessons(LoginUser user, UUID sectionId, List<UUID> orderedIds);

	// ----------------------------------------------------------------- học liệu

	/** Thêm học liệu dạng link ngoài (URL bắt buộc). */
	CourseDetailResponse addMaterial(LoginUser user, UUID lessonId, MaterialRequest request);

	/**
	 * Upload file (video, tài liệu, hình ảnh) lên MinIO rồi gắn vào bài học (UC-C11).
	 * {@code name} / {@code type} có thể null: mặc định lấy tên file và đoán loại theo MIME type.
	 */
	CourseDetailResponse uploadMaterial(LoginUser user, UUID lessonId, MultipartFile file, String name, MaterialType type);

	CourseDetailResponse updateMaterial(LoginUser user, UUID materialId, MaterialRequest request);

	CourseDetailResponse deleteMaterial(LoginUser user, UUID materialId);
}
