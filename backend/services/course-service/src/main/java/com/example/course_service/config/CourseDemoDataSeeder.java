package com.example.course_service.config;

import java.util.UUID;

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
import com.example.course_service.entity.Material;
import com.example.course_service.entity.MaterialType;
import com.example.course_service.entity.Section;
import com.example.course_service.repository.CourseRepository;
import com.example.course_service.repository.LessonRepository;
import com.example.course_service.repository.MaterialRepository;
import com.example.course_service.repository.SectionRepository;

import lombok.RequiredArgsConstructor;

/**
 * Dữ liệu mẫu cho màn hình quản lý khoá học: 1 khoá DRAFT có curriculum + học liệu và 1 khoá đã xuất bản.
 * Độc lập với {@link DemoDataSeeder} (seed quiz). Tắt bằng SEED_DEMO_DATA=false.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
@Transactional
public class CourseDemoDataSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(CourseDemoDataSeeder.class);

	private final CourseRepository courseRepository;
	private final SectionRepository sectionRepository;
	private final LessonRepository lessonRepository;
	private final MaterialRepository materialRepository;

	static final String REACT_TITLE = "ReactJS từ cơ bản đến nâng cao";
	static final String DOCKER_TITLE = "Docker cho lập trình viên";

	/**
	 * Mỗi khoá mẫu chỉ được tạo khi chưa có khoá cùng tên, nên khởi động lại bao nhiêu lần cũng không bị trùng
	 * (kể cả khi người dùng đã xoá hết học liệu).
	 */
	@Override
	public void run(ApplicationArguments args) {
		if (!courseRepository.existsByTitle(REACT_TITLE)) {
			seedReactCourse();
		}
		if (!courseRepository.existsByTitle(DOCKER_TITLE)) {
			seedDockerCourse();
		}
	}

	private void seedReactCourse() {
		Course react = courseRepository.save(Course.builder()
				.instructorId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
				.title(REACT_TITLE)
				.description("Xây dựng giao diện web hiện đại với React, Hooks và Vite.")
				.thumbnailUrl("https://placehold.co/600x340?text=ReactJS")
				.status(CourseStatus.DRAFT)
				.build());

		Section intro = section(react, "Chương 1: Làm quen React", 1);
		Lesson jsx = lesson(intro, "Bài 1: JSX và component", LessonType.VIDEO, 1, true);
		lesson(intro, "Bài 2: Props và state", LessonType.ARTICLE, 2, false);
		Section hooks = section(react, "Chương 2: Hooks", 2);
		Lesson useEffect = lesson(hooks, "Bài 3: useEffect", LessonType.VIDEO, 1, false);

		material(jsx, "Video bài giảng JSX", MaterialType.VIDEO,
				"https://example.com/videos/react-jsx.mp4", 52_428_800L, "video/mp4", 1);
		material(jsx, "Slide chương 1", MaterialType.DOCUMENT,
				"https://example.com/docs/react-chuong-1.pdf", 1_048_576L, "application/pdf", 2);
		material(useEffect, "Video useEffect", MaterialType.VIDEO,
				"https://example.com/videos/react-use-effect.mp4", 73_400_320L, "video/mp4", 1);
		log.info("Đã tạo khoá học mẫu '{}'", react.getTitle());
	}

	private void seedDockerCourse() {
		Course docker = courseRepository.save(Course.builder()
				.instructorId(UUID.fromString("44444444-4444-4444-4444-444444444444"))
				.title(DOCKER_TITLE)
				.description("Đóng gói và triển khai ứng dụng bằng Docker và Docker Compose.")
				.thumbnailUrl("https://placehold.co/600x340?text=Docker")
				.status(CourseStatus.PUBLISHED)
				.build());
		Section basics = section(docker, "Chương 1: Container là gì?", 1);
		lesson(basics, "Bài 1: Image và container", LessonType.VIDEO, 1, true);

		log.info("Đã tạo khoá học mẫu '{}'", docker.getTitle());
	}

	private Section section(Course course, String title, int position) {
		return sectionRepository.save(Section.builder().course(course).title(title).position(position).build());
	}

	private Lesson lesson(Section section, String title, LessonType type, int position, boolean preview) {
		return lessonRepository.save(Lesson.builder()
				.section(section).title(title).type(type).position(position).isPreview(preview).build());
	}

	private void material(Lesson lesson, String name, MaterialType type, String url, long size, String mime,
			int position) {
		materialRepository.save(Material.builder()
				.lesson(lesson).name(name).type(type).url(url).fileSize(size).mimeType(mime).position(position)
				.build());
	}
}
