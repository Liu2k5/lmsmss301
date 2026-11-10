package com.example.course_service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.course_service.dto.CategoryCountDto;
import com.example.course_service.entity.Course;
import com.example.course_service.entity.CourseStatus;

public interface CourseRepository extends JpaRepository<Course, UUID> {

	/**
	 * Tìm khoá học theo bộ lọc tuỳ chọn. {@code keywordPattern} phải ở dạng {@code %từ khoá%} viết thường,
	 * truyền {@code null} để bỏ qua điều kiện tương ứng.
	 */
	@Query("""
			select c from Course c
			left join fetch c.category
			where (:status is null or c.status = :status)
			  and (:instructorId is null or c.instructorId = :instructorId)
			  and (:categoryId is null or c.category.id = :categoryId)
			  and (:keywordPattern is null
			       or lower(c.title) like :keywordPattern
			       or lower(coalesce(c.description, '')) like :keywordPattern)
			order by c.updatedAt desc, c.id desc
			""")
	List<Course> search(@Param("keywordPattern") String keywordPattern,
			@Param("status") CourseStatus status,
			@Param("instructorId") UUID instructorId,
			@Param("categoryId") UUID categoryId);

	boolean existsByTitle(String title);

	/** Giảng viên đã có khoá học trùng tên chưa (không phân biệt hoa thường) — dùng khi tạo mới. */
	boolean existsByInstructorIdAndTitleIgnoreCase(UUID instructorId, String title);

	/** Như trên nhưng bỏ qua chính khoá học đang sửa. */
	boolean existsByInstructorIdAndTitleIgnoreCaseAndIdNot(UUID instructorId, String title, UUID id);

	/** Số khoá học của từng category (category không có khoá nào sẽ không xuất hiện). */
	@Query("""
			select new com.example.course_service.dto.CategoryCountDto(c.category.id, count(c))
			from Course c
			where c.category is not null
			group by c.category.id
			""")
	List<CategoryCountDto> countByCategory();

	/** Số khoá học thuộc một category (dùng khi xem chi tiết / trước khi xoá category). */
	long countByCategoryId(UUID categoryId);
}
