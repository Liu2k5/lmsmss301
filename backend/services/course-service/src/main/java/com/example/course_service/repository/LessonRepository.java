package com.example.course_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.course_service.entity.Lesson;
import com.example.course_service.entity.LessonType;

public interface LessonRepository extends JpaRepository<Lesson, Integer> {

	@Query("""
			select l from Lesson l
			  join fetch l.section s
			  join fetch s.course c
			where l.type = :type
			order by c.title asc, s.position asc, l.position asc
			""")
	List<Lesson> findAllWithContextByType(@Param("type") LessonType type);
}
