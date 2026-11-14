package com.example.course_service.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.example.course_service.client.EnrollmentClient;
import com.example.course_service.entity.LessonType;
import com.example.course_service.entity.Quiz;
import com.example.course_service.repository.CourseRepository;
import com.example.course_service.repository.LessonRepository;
import com.example.course_service.repository.MaterialRepository;
import com.example.course_service.repository.QuizRepository;

/** Bật seed dữ liệu mẫu: các seeder phải chạy được, tạo đủ dữ liệu và chạy lại không bị trùng. */
@SpringBootTest(properties = "app.seed-demo-data=true")
@Transactional
class CatalogDemoDataSeederTest {

	@Autowired
	private CatalogDemoDataSeeder seeder;

	@Autowired
	private CourseRepository courseRepository;

	@Autowired
	private LessonRepository lessonRepository;

	@Autowired
	private MaterialRepository materialRepository;

	@Autowired
	private QuizRepository quizRepository;

	@MockitoBean
	private EnrollmentClient enrollmentClient;

	@Test
	void seedsCatalogAndIsIdempotent() {
		long courses = courseRepository.count();
		long quizzes = quizRepository.count();
		long materials = materialRepository.count();

		// 3 khoá cũ + 6 khoá mới; 3 quiz cũ + 13 quiz mới.
		assertThat(courses).isEqualTo(9);
		assertThat(quizzes).isEqualTo(16);
		assertThat(courseRepository.existsByTitle("Python cho người mới bắt đầu")).isTrue();
		assertThat(quizRepository.findAll()).allSatisfy((Quiz quiz) -> {
			assertThat(quiz.getLesson().getType()).isEqualTo(LessonType.QUIZ);
			assertThat(quiz.getQuestions()).isNotEmpty().allSatisfy(question ->
					assertThat(question.getOptions()).anySatisfy(option -> assertThat(option.isCorrect()).isTrue()));
		});

		seeder.run(new DefaultApplicationArguments());

		assertThat(courseRepository.count()).isEqualTo(courses);
		assertThat(quizRepository.count()).isEqualTo(quizzes);
		assertThat(materialRepository.count()).isEqualTo(materials);
		assertThat(lessonRepository.count()).isPositive();
	}
}
