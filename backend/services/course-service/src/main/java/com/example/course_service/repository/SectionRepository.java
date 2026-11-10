package com.example.course_service.repository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.course_service.entity.Section;

public interface SectionRepository extends JpaRepository<Section, UUID> {

	List<Section> findByCourseIdOrderByPositionAscIdAsc(UUID courseId);

	@Query("select s from Section s join fetch s.course where s.id = :id")
	Optional<Section> findByIdWithCourse(@Param("id") UUID id);

	@Query("select coalesce(max(s.position), 0) from Section s where s.course.id = :courseId")
	int findMaxPosition(@Param("courseId") UUID courseId);

	/** Khoá học đã có chương trùng tên chưa (không phân biệt hoa thường) — dùng khi thêm chương. */
	boolean existsByCourseIdAndTitleIgnoreCase(UUID courseId, String title);

	/** Như trên nhưng bỏ qua chính chương đang sửa. */
	boolean existsByCourseIdAndTitleIgnoreCaseAndIdNot(UUID courseId, String title, UUID id);
}
