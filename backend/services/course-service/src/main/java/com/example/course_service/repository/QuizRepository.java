package com.example.course_service.repository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.course_service.entity.Quiz;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {

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
	Optional<Quiz> findByLessonIdWithContext(@Param("lessonId") UUID lessonId);

	@Query("""
			select q from Quiz q
			  join fetch q.lesson l
			  join fetch l.section s
			  join fetch s.course c
			where q.id = :id
			""")
	Optional<Quiz> findByIdWithContext(@Param("id") UUID id);

	boolean existsByLessonId(UUID lessonId);

	/** Dùng cho phần quản lý khoá học: biết bài học nào đã có quiz, và xoá quiz khi xoá bài học. */
	List<Quiz> findByLessonIdIn(java.util.Collection<UUID> lessonIds);

	boolean existsByLessonIdAndIdNot(UUID lessonId, UUID id);
}
