package com.example.course_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.course_service.entity.Course;
import com.example.course_service.entity.CourseStatus;

public interface CourseRepository extends JpaRepository<Course, Integer> {

	/**
	 * Tìm khoá học theo bộ lọc tuỳ chọn. {@code keywordPattern} phải ở dạng {@code %từ khoá%} viết thường,
	 * truyền {@code null} để bỏ qua điều kiện tương ứng.
	 */
	@Query("""
			select c from Course c
			where (:status is null or c.status = :status)
			  and (:instructorId is null or c.instructorId = :instructorId)
			  and (:keywordPattern is null
			       or lower(c.title) like :keywordPattern
			       or lower(coalesce(c.description, '')) like :keywordPattern)
			order by c.updatedAt desc, c.id desc
			""")
	List<Course> search(@Param("keywordPattern") String keywordPattern,
			@Param("status") CourseStatus status,
			@Param("instructorId") Integer instructorId);

	boolean existsByTitle(String title);
}
