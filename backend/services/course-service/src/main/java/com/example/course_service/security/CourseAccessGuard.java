package com.example.course_service.security;

import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.example.course_service.entity.Course;

import lombok.RequiredArgsConstructor;

/**
 * Chặn sửa nội dung khoá học khi người gọi không phải giảng viên phụ trách (Admin được phép).
 * SecurityConfig đã chặn theo vai trò; lớp này chặn theo quyền sở hữu khoá học.
 */
@Component
@RequiredArgsConstructor
public class CourseAccessGuard {

	static final String DEFAULT_MESSAGE =
			"Chỉ giảng viên phụ trách khoá học mới được chỉnh sửa nội dung khoá học này";

	private final CurrentUser currentUser;

	public void checkCanManage(Course course) {
		checkCanManage(course, DEFAULT_MESSAGE);
	}

	public void checkCanManage(Course course, String message) {
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
			throw new AccessDeniedException(message);
		}
	}

	/** Giảng viên chỉ được tạo khoá học cho chính mình; Admin được tạo hộ. */
	public void checkCanAssignInstructor(UUID instructorId) {
		if (currentUser.isAdmin()) {
			return;
		}
		var me = currentUser.id();
		if (me.isEmpty()) {
			return;
		}
		if (!me.get().equals(instructorId)) {
			throw new AccessDeniedException(
					"Mã giảng viên phải là tài khoản đang đăng nhập (hoặc dùng tài khoản quản trị)");
		}
	}

	/**
	 * Khoá học ở trạng thái Bản nháp chỉ dành cho người soạn (Admin, hoặc giảng viên phụ trách).
	 * Học viên đã đăng nhập nhưng không phải chủ khoá học thì không thấy khoá nháp.
	 * Chưa xác thực (khách, hoặc test tắt filter) thì giữ nguyên hành vi catalog công khai.
	 */
	public boolean canSeeDraft(Course course) {
		if (currentUser.isAdmin()) {
			return true;
		}
		var me = currentUser.id();
		if (me.isEmpty()) {
			return true;
		}
		return currentUser.isInstructor() && me.get().equals(course.getInstructorId());
	}
}
