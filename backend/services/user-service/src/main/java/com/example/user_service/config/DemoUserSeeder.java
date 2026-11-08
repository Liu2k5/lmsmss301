package com.example.user_service.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.user_service.entity.Role;
import com.example.user_service.entity.University;
import com.example.user_service.entity.User;
import com.example.user_service.repository.UniversityRepository;
import com.example.user_service.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Đồng bộ hồ sơ của các tài khoản demo đã seed sẵn trong Keycloak
 * (student1 / instructor1 / admin1) vào user_db, để các endpoint như
 * {@code GET /api/v1/users/me} trả được dữ liệu ngay sau khi đăng nhập.
 *
 * <p>Chỉ tạo bản ghi còn thiếu, không ghi đè hồ sơ người dùng đã sửa.
 * Tắt bằng {@code SEED_DEMO_DATA=false}.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true", matchIfMissing = true)
public class DemoUserSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DemoUserSeeder.class);

	private final UserRepository userRepository;
	private final UniversityRepository universityRepository;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		University university = universityRepository.findAll().stream()
				.findFirst()
				.orElseGet(() -> {
					University created = new University();
					created.setName("Đại học FPT");
					created.setAddress("Khu công nghệ cao Hòa Lạc, Hà Nội");
					return universityRepository.save(created);
				});

		List<User> demoUsers = List.of(
				demo("student1", "Student One", "student1@lms.local", Role.STUDENT, university),
				demo("instructor1", "Instructor One", "instructor1@lms.local", Role.INSTRUCTOR, university),
				demo("admin1", "Admin One", "admin1@lms.local", Role.ADMINISTRATOR, university));

		int created = 0;
		for (User user : demoUsers) {
			if (userRepository.findById(user.getUsername()).isEmpty()) {
				userRepository.save(user);
				created++;
			}
		}

		if (created > 0) {
			log.info("Đã đồng bộ {} hồ sơ người dùng demo từ Keycloak vào user_db", created);
		}
	}

	private User demo(String username, String fullname, String email, Role role, University university) {
		User user = new User();
		user.setUsername(username);
		user.setFullname(fullname);
		user.setEmail(email);
		user.setRole(role);
		user.setUniversity(university);
		user.setEnabled(true);
		return user;
	}
}
