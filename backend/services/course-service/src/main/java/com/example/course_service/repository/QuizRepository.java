package com.example.course_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.course_service.entity.Quiz;

public interface QuizRepository extends JpaRepository<Quiz, Integer> {

	@Query("""
			select q from Quiz q
			  join fetch q.lesson l
			  join fetch l.section s
			  join fetch s.course c
			order by q.updatedAt desc
			""")
	List<Quiz> findAllWithContext();

	@Query("""
			select q from Quiz q
			  join fetch q.lesson l
			  join fetch l.section s
			  join fetch s.course c
			where l.id = :lessonId
			""")
	Optional<Quiz> findByLessonIdWithContext(@Param("lessonId") Integer lessonId);

	@Query("""
			select q from Quiz q
			  join fetch q.lesson l
			  join fetch l.section s
			  join fetch s.course c
			where q.id = :id
			""")
	Optional<Quiz> findByIdWithContext(@Param("id") Integer id);

	boolean existsByLessonId(Integer lessonId);

	boolean existsByLessonIdAndIdNot(Integer lessonId, Integer id);
}
