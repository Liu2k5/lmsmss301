package com.example.course_service.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.course_service.entity.Material;

public interface MaterialRepository extends JpaRepository<Material, Integer> {

	@Query("""
			select m from Material m
			where m.lesson.section.course.id = :courseId
			order by m.position asc, m.id asc
			""")
	List<Material> findByCourseIdOrdered(@Param("courseId") Integer courseId);

	@Query("""
			select m from Material m
			  join fetch m.lesson l
			  join fetch l.section s
			  join fetch s.course c
			where m.id = :id
			""")
	Optional<Material> findByIdWithContext(@Param("id") Integer id);

	List<Material> findByLessonIdIn(Collection<Integer> lessonIds);

	@Query("select coalesce(max(m.position), 0) from Material m where m.lesson.id = :lessonId")
	int findMaxPosition(@Param("lessonId") Integer lessonId);
}
