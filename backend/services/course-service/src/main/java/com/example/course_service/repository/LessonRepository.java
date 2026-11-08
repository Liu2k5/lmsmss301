package com.example.course_service.repository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.course_service.entity.Lesson;
import com.example.course_service.entity.LessonType;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {

	@Query("""
			select l from Lesson l
			  join fetch l.section s
			  join fetch s.course c
			where l.type = :type
			order by c.title asc, s.position asc, l.position asc
			""")
	List<Lesson> findAllWithContextByType(@Param("type") LessonType type);

	@Query("""
			select l from Lesson l
			  join fetch l.section s
			where s.course.id = :courseId
			order by s.position asc, l.position asc, l.id asc
			""")
	List<Lesson> findByCourseIdOrdered(@Param("courseId") UUID courseId);

	List<Lesson> findBySectionIdOrderByPositionAscIdAsc(UUID sectionId);

	@Query("""
			select l from Lesson l
			  join fetch l.section s
			  join fetch s.course c
			where l.id = :id
			""")
	Optional<Lesson> findByIdWithContext(@Param("id") UUID id);

	@Query("select coalesce(max(l.position), 0) from Lesson l where l.section.id = :sectionId")
	int findMaxPosition(@Param("sectionId") UUID sectionId);

	@Query("select l.section.course.id as courseId, count(l) as total from Lesson l group by l.section.course.id")
	List<CourseCount> countByCourse();
}
