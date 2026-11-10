package com.example.course_service.support;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Dùng cho các test nghiệp vụ chạy với {@code @AutoConfigureMockMvc(addFilters = false)}:
 * không có filter đọc JWT nên phải tự đặt người dùng vào SecurityContext,
 * để {@code @PreAuthorize} và {@code JwtAuthenticationToken} trên controller hoạt động.
 */
public final class TestLogin {

	public static final String ADMIN_ID = "33333333-3333-3333-3333-333333333333";

	private TestLogin() {
	}

	/** Giảng viên đứng tên các khoá học trong test nghiệp vụ và dữ liệu mẫu. */
	public static final String INSTRUCTOR_ID = "22222222-2222-2222-2222-222222222222";

	/** Đăng nhập bằng tài khoản Admin (được sửa mọi khoá học, nhưng không xem được khoá nháp). */
	public static void asAdmin() {
		login(ADMIN_ID, "admin");
	}

	/** Đăng nhập bằng giảng viên {@link #INSTRUCTOR_ID} (chủ khoá học → được sửa và xem khoá nháp). */
	public static void asInstructor() {
		login(INSTRUCTOR_ID, "instructor");
	}

	private static void login(String userId, String role) {
		Jwt jwt = Jwt.withTokenValue("test-token")
				.header("alg", "none")
				.subject(userId)
				.claim("realm_access", Map.of("roles", List.of(role)))
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(3600))
				.build();
		JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt,
				List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())));
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	public static void logout() {
		SecurityContextHolder.clearContext();
	}
}
