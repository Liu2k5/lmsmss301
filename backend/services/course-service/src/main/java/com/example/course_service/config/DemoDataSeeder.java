package com.example.course_service.config;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.course_service.entity.Course;
import com.example.course_service.entity.CourseStatus;
import com.example.course_service.entity.Lesson;
import com.example.course_service.entity.LessonType;
import com.example.course_service.entity.Option;
import com.example.course_service.entity.Question;
import com.example.course_service.entity.QuestionType;
import com.example.course_service.entity.Quiz;
import com.example.course_service.entity.Section;
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
	private final SectionRepository sectionRepository;
	private final LessonRepository lessonRepository;
	private final QuizRepository quizRepository;

	@Override
	public void run(ApplicationArguments args) {
		if (quizRepository.count() > 0) {
			return;
		}

		Course springCourse = courseRepository.save(Course.builder()
				.instructorId(1)
				.title("Lập trình Java Spring Boot")
				.description("Khoá học nhập môn Spring Boot dành cho sinh viên.")
				.thumbnailUrl("https://placehold.co/600x340?text=Spring+Boot")
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

		lessonRepository.save(Lesson.builder()
				.section(chapter2)
				.title("Bài 3: Kiểm tra cuối chương 2")
				.description("Bài kiểm tra chưa được tạo quiz — dùng để thử chức năng tạo mới.")
				.type(LessonType.QUIZ)
				.position(1)
				.isPreview(false)
				.build());

		log.info("Đã tạo dữ liệu mẫu: course='{}', lesson video='{}', quiz='{}'",
				springCourse.getTitle(), videoLesson.getTitle(), quiz.getTitle());
	}
}
