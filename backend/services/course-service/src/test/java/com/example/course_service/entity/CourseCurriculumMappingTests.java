package com.example.course_service.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import jakarta.persistence.PersistenceException;

@DataJpaTest
class CourseCurriculumMappingTests {

	@Autowired
	private TestEntityManager em;

	@Test
	void luuVaDocLaiDuocToanBoCayCurriculm() {
		Course course = em.persist(Course.builder()
				.instructorId(7)
				.title("Spring Boot co ban")
				.description("Khoa hoc nhap mon")
				.thumbnailUrl("https://cdn.local/thumb.png")
				.status(CourseStatus.DRAFT)
				.build());

		Section section = em.persist(Section.builder()
				.course(course)
				.title("Chuong 1")
				.position(1)
				.build());

		Lesson videoLesson = em.persist(Lesson.builder()
				.section(section)
				.title("Bai 1 - Gioi thieu")
				.type(LessonType.VIDEO)
				.position(1)
				.isPreview(true)
				.build());

		Material material = em.persist(Material.builder()
				.lesson(videoLesson)
				.name("bai1.mp4")
				.type(MaterialType.VIDEO)
				.url("https://cdn.local/bai1.mp4")
				.fileSize(1048576L)
				.mimeType("video/mp4")
				.position(1)
				.build());

		Lesson quizLesson = em.persist(Lesson.builder()
				.section(section)
				.title("Bai 2 - Kiem tra")
				.type(LessonType.QUIZ)
				.position(2)
				.isPreview(false)
				.build());

		Quiz quiz = em.persist(Quiz.builder()
				.lesson(quizLesson)
				.title("Quiz chuong 1")
				.passingScore(new BigDecimal("70.00"))
				.build());

		Question question = em.persist(Question.builder()
				.quiz(quiz)
				.content("Spring Boot la gi?")
				.type(QuestionType.SINGLE_CHOICE)
				.position(1)
				.build());

		Option correct = em.persist(Option.builder()
				.question(question)
				.content("Framework cua Spring")
				.isCorrect(true)
				.position(1)
				.build());

		em.persist(Option.builder()
				.question(question)
				.content("Ngon ngu lap trinh")
				.isCorrect(false)
				.position(2)
				.build());

		em.flush();
		em.clear();

		Course reloadedCourse = em.find(Course.class, course.getId());
		assertThat(reloadedCourse).isNotNull();
		assertThat(reloadedCourse.getStatus()).isEqualTo(CourseStatus.DRAFT);
		assertThat(reloadedCourse.getInstructorId()).isEqualTo(7);
		assertThat(reloadedCourse.getCreatedAt()).isNotNull();
		assertThat(reloadedCourse.getUpdatedAt()).isNotNull();

		Material reloadedMaterial = em.find(Material.class, material.getId());
		assertThat(reloadedMaterial.getLesson().getId()).isEqualTo(videoLesson.getId());
		assertThat(reloadedMaterial.getFileSize()).isEqualTo(1048576L);
		assertThat(reloadedMaterial.getType()).isEqualTo(MaterialType.VIDEO);
		assertThat(reloadedMaterial.getLesson().isPreview()).isTrue();

		Quiz reloadedQuiz = em.find(Quiz.class, quiz.getId());
		assertThat(reloadedQuiz.getLesson().getId()).isEqualTo(quizLesson.getId());
		assertThat(reloadedQuiz.getPassingScore()).isEqualByComparingTo("70.00");

		Option reloadedOption = em.find(Option.class, correct.getId());
		assertThat(reloadedOption.isCorrect()).isTrue();
		assertThat(reloadedOption.getQuestion().getId()).isEqualTo(question.getId());

		assertThat(countOf("select count(s) from Section s")).isEqualTo(1L);
		assertThat(countOf("select count(l) from Lesson l")).isEqualTo(2L);
		assertThat(countOf("select count(m) from Material m")).isEqualTo(1L);
		assertThat(countOf("select count(q) from Quiz q")).isEqualTo(1L);
		assertThat(countOf("select count(q) from Question q")).isEqualTo(1L);
		assertThat(countOf("select count(o) from Option o")).isEqualTo(2L);
	}

	@Test
	void khongChoPhepHaiQuizTrenCungMotLesson() {
		Course course = em.persist(Course.builder()
				.instructorId(1).title("Course").status(CourseStatus.PUBLISHED).build());
		Section section = em.persist(Section.builder().course(course).title("S").position(1).build());
		Lesson lesson = em.persist(Lesson.builder()
				.section(section).title("L").type(LessonType.QUIZ).position(1).isPreview(false).build());

		em.persist(Quiz.builder().lesson(lesson).title("Quiz 1")
				.passingScore(new BigDecimal("50.00")).build());
		em.flush();

		// IDENTITY nên INSERT chạy ngay tại persist(), không đợi flush().
		assertThatThrownBy(() -> {
			em.persist(Quiz.builder().lesson(lesson).title("Quiz 2")
					.passingScore(new BigDecimal("60.00")).build());
			em.flush();
		}).isInstanceOf(PersistenceException.class);
	}

	private long countOf(String jpql) {
		return em.getEntityManager().createQuery(jpql, Long.class).getSingleResult();
	}
}
