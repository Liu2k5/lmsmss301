package com.example.course_service.config;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.course_service.entity.Category;
import com.example.course_service.entity.Course;
import com.example.course_service.entity.CourseStatus;
import com.example.course_service.entity.Lesson;
import com.example.course_service.entity.LessonType;
import com.example.course_service.entity.Option;
import com.example.course_service.entity.Question;
import com.example.course_service.entity.QuestionType;
import com.example.course_service.entity.Quiz;
import com.example.course_service.entity.Section;
import com.example.course_service.repository.CategoryRepository;
import com.example.course_service.repository.CourseRepository;
import com.example.course_service.repository.LessonRepository;
import com.example.course_service.repository.QuizRepository;
import com.example.course_service.repository.SectionRepository;

import lombok.RequiredArgsConstructor;

/**
 * Tạo dữ liệu mẫu để thử giao diện CRUD quiz khi database còn trống.
 * Tắt bằng {@code app.seed-demo-data=false} hoặc biến môi trường {@code SEED_DEMO_DATA=false}.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
@Transactional
public class DemoDataSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

	private final CourseRepository courseRepository;
	private final CategoryRepository categoryRepository;
	private final SectionRepository sectionRepository;
	private final LessonRepository lessonRepository;
	private final QuizRepository quizRepository;

	@Override
	public void run(ApplicationArguments args) {
		if (quizRepository.count() > 0) {
			return;
		}

		Course springCourse = courseRepository.save(Course.builder()
				.instructorId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
				.title("Lập trình Java Spring Boot")
				.description("Khoá học nhập môn Spring Boot dành cho sinh viên.")
				.thumbnailUrl("https://placehold.co/600x340?text=Spring+Boot")
				.category(category("Lập trình Backend", "Xây dựng API và xử lý phía server"))
				.status(CourseStatus.PUBLISHED)
				.build());

		Section chapter1 = sectionRepository.save(Section.builder()
				.course(springCourse)
				.title("Chương 1: Tổng quan")
				.description("Giới thiệu Spring Boot và môi trường phát triển.")
				.position(1)
				.build());

		Lesson videoLesson = lessonRepository.save(Lesson.builder()
				.section(chapter1)
				.title("Bài 1: Spring Boot là gì?")
				.description("Video giới thiệu tổng quan.")
				.type(LessonType.VIDEO)
				.position(1)
				.isPreview(true)
				.build());

		Lesson quizLesson = lessonRepository.save(Lesson.builder()
				.section(chapter1)
				.title("Bài 2: Kiểm tra cuối chương 1")
				.description("Bài kiểm tra 2 câu hỏi.")
				.type(LessonType.QUIZ)
				.position(2)
				.isPreview(false)
				.build());

		Quiz quiz = Quiz.builder()
				.lesson(quizLesson)
				.title("Kiểm tra cuối chương 1")
				.description("Chọn đáp án đúng nhất.")
				.passingScore(new BigDecimal("70.00"))
				.maxAttempts(3)
				.build();

		Question q1 = Question.builder()
				.content("Spring Boot là gì?")
				.type(QuestionType.SINGLE_CHOICE)
				.position(1)
				.build();
		q1.addOption(Option.builder().content("Một framework giúp đơn giản hoá việc phát triển ứng dụng Spring")
				.isCorrect(true).position(1).build());
		q1.addOption(Option.builder().content("Một ngôn ngữ lập trình")
				.isCorrect(false).position(2).build());
		q1.addOption(Option.builder().content("Một hệ quản trị cơ sở dữ liệu")
				.isCorrect(false).position(3).build());
		quiz.addQuestion(q1);

		Question q2 = Question.builder()
				.content("Annotation nào dùng để khai báo lớp khởi động ứng dụng Spring Boot?")
				.type(QuestionType.SINGLE_CHOICE)
				.position(2)
				.build();
		q2.addOption(Option.builder().content("@SpringBootApplication")
				.isCorrect(true).position(1).build());
		q2.addOption(Option.builder().content("@EnableJpa")
				.isCorrect(false).position(2).build());
		q2.addOption(Option.builder().content("@RestController")
				.isCorrect(false).position(3).build());
		quiz.addQuestion(q2);

		quizRepository.save(quiz);

		Section chapter2 = sectionRepository.save(Section.builder()
				.course(springCourse)
				.title("Chương 2: REST API")
				.description("Xây dựng REST API với Spring MVC.")
				.position(2)
				.build());

		Lesson restQuizLesson = lessonRepository.save(Lesson.builder()
				.section(chapter2)
				.title("Bài 3: Kiểm tra cuối chương 2")
				.description("Bài kiểm tra 3 câu hỏi về REST API.")
				.type(LessonType.QUIZ)
				.position(1)
				.isPreview(false)
				.build());

		Quiz restQuiz = Quiz.builder()
				.lesson(restQuizLesson)
				.title("Kiểm tra cuối chương 2")
				.description("REST API, HTTP method và status code.")
				.passingScore(new BigDecimal("60.00"))
				.maxAttempts(5)
				.build();

		Question q3 = Question.builder()
				.content("Trong Spring MVC, annotation nào dùng để khai báo một REST controller?")
				.type(QuestionType.SINGLE_CHOICE)
				.position(1)
				.build();
		q3.addOption(Option.builder().content("@RestController").isCorrect(true).position(1).build());
		q3.addOption(Option.builder().content("@ControllerAdvice").isCorrect(false).position(2).build());
		q3.addOption(Option.builder().content("@Entity").isCorrect(false).position(3).build());
		restQuiz.addQuestion(q3);

		Question q4 = Question.builder()
				.content("HTTP method nào dùng để thay thế toàn bộ một tài nguyên?")
				.type(QuestionType.SINGLE_CHOICE)
				.position(2)
				.build();
		q4.addOption(Option.builder().content("PUT").isCorrect(true).position(1).build());
		q4.addOption(Option.builder().content("GET").isCorrect(false).position(2).build());
		q4.addOption(Option.builder().content("DELETE").isCorrect(false).position(3).build());
		restQuiz.addQuestion(q4);

		Question q5 = Question.builder()
				.content("Những status code nào thể hiện yêu cầu đã xử lý thành công? (chọn nhiều)")
				.type(QuestionType.MULTIPLE_CHOICE)
				.position(3)
				.build();
		q5.addOption(Option.builder().content("200 OK").isCorrect(true).position(1).build());
		q5.addOption(Option.builder().content("201 Created").isCorrect(true).position(2).build());
		q5.addOption(Option.builder().content("204 No Content").isCorrect(true).position(3).build());
		q5.addOption(Option.builder().content("404 Not Found").isCorrect(false).position(4).build());
		restQuiz.addQuestion(q5);

		quizRepository.save(restQuiz);

		Lesson finalQuizLesson = lessonRepository.save(Lesson.builder()
				.section(chapter2)
				.title("Bài 4: Kiểm tra tổng hợp Spring Boot")
				.description("Bài kiểm tra cuối khoá: đúng/sai, một đáp án và nhiều đáp án.")
				.type(LessonType.QUIZ)
				.position(2)
				.isPreview(false)
				.build());

		Quiz finalQuiz = Quiz.builder()
				.lesson(finalQuizLesson)
				.title("Kiểm tra tổng hợp Spring Boot")
				.description("Tổng hợp kiến thức Spring Boot và Spring MVC.")
				.passingScore(new BigDecimal("70.00"))
				.maxAttempts(3)
				.build();

		Question q6 = Question.builder()
				.content("@SpringBootApplication là tổ hợp của @Configuration, @EnableAutoConfiguration "
						+ "và @ComponentScan.")
				.type(QuestionType.TRUE_FALSE)
				.position(1)
				.build();
		q6.addOption(Option.builder().content("Đúng").isCorrect(true).position(1).build());
		q6.addOption(Option.builder().content("Sai").isCorrect(false).position(2).build());
		finalQuiz.addQuestion(q6);

		Question q7 = Question.builder()
				.content("Cấu hình nào khai báo cổng chạy của ứng dụng trong application.properties?")
				.type(QuestionType.SINGLE_CHOICE)
				.position(2)
				.build();
		q7.addOption(Option.builder().content("server.port=8080").isCorrect(true).position(1).build());
		q7.addOption(Option.builder().content("app.port=8080").isCorrect(false).position(2).build());
		q7.addOption(Option.builder().content("spring.port=8080").isCorrect(false).position(3).build());
		finalQuiz.addQuestion(q7);

		Question q8 = Question.builder()
				.content("Những cách nào có thể inject dependency vào một Spring bean? (chọn nhiều)")
				.type(QuestionType.MULTIPLE_CHOICE)
				.position(3)
				.build();
		q8.addOption(Option.builder().content("Constructor injection").isCorrect(true).position(1).build());
		q8.addOption(Option.builder().content("Setter injection").isCorrect(true).position(2).build());
		q8.addOption(Option.builder().content("Field injection").isCorrect(true).position(3).build());
		q8.addOption(Option.builder().content("Khởi tạo bằng new ở mọi nơi cần dùng")
				.isCorrect(false).position(4).build());
		finalQuiz.addQuestion(q8);

		quizRepository.save(finalQuiz);

		log.info("Đã tạo dữ liệu mẫu: course='{}', lesson video='{}', quiz='{}', quiz='{}', quiz='{}'",
				springCourse.getTitle(), videoLesson.getTitle(),
				quiz.getTitle(), restQuiz.getTitle(), finalQuiz.getTitle());
	}

	/** Lấy danh mục mẫu theo tên, chưa có thì tạo mới (chạy lại nhiều lần không bị trùng). */
	private Category category(String name, String description) {
		Optional<Category> existing = categoryRepository.findByNameIgnoreCase(name);
		if (existing.isPresent()) {
			return existing.get();
		}
		return categoryRepository.save(Category.builder().name(name).description(description).build());
	}
}
