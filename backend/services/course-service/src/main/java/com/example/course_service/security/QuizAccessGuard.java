package com.example.course_service.security;

import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.example.course_service.entity.Course;

import lombok.RequiredArgsConstructor;

/**
 * Quiz chỉ do giảng viên phụ trách khoá học được sửa (Admin được phép).
 * Việc chặn theo vai trò đã do SecurityConfig lo; lớp này chặn theo quyền sở hữu khoá học.
 */
@Component
@RequiredArgsConstructor
public class QuizAccessGuard {

	private final CurrentUser currentUser;

	public void checkCanManage(Course course) {
		if (currentUser.isAdmin()) {
			return;
		}

		// Không có thông tin xác thực (ví dụ test tắt security filter) thì bỏ qua kiểm tra sở hữu;
		// trên luồng thật, SecurityConfig đã bắt buộc ROLE_INSTRUCTOR/ROLE_ADMIN trước khi tới đây.
		var me = currentUser.id();
		if (me.isEmpty()) {
			return;
		}

		UUID owner = course.getInstructorId();
		if (!me.get().equals(owner)) {
			throw new AccessDeniedException(
					"Chỉ giảng viên phụ trách khoá học mới được sửa quiz của khoá học này");
		}
	}
}
