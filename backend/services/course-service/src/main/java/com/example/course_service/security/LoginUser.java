package com.example.course_service.security;

import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import lombok.extern.slf4j.Slf4j;

/**
 * Người đang gọi API, tạo ở controller từ {@link JwtAuthenticationToken} rồi truyền xuống service.
 * <p>
 * Role không đọc lại từ JWT mà lấy từ authorities mà {@link KeycloakRoleConverter} đã chuyển sẵn
 * (ROLE_INSTRUCTOR, ROLE_ADMIN…), nên {@code LoginUser} và {@code SecurityConfig} luôn hiểu role giống nhau.
 *
 * @param id         UUID của user (claim "sub" trong JWT); null nếu chưa đăng nhập
 * @param admin      có role admin không
 * @param instructor có role instructor không
 */
@Slf4j
public record LoginUser(UUID id, boolean admin, boolean instructor) {

	public static LoginUser from(JwtAuthenticationToken auth) {
		if (auth == null) {
			return new LoginUser(null, false, false);
		}

		UUID id = parseId(auth.getToken().getSubject());

		boolean admin = false;
		boolean instructor = false;
		for (GrantedAuthority authority : auth.getAuthorities()) {
			String name = authority.getAuthority();   
			if (name.equals("ROLE_ADMIN") || name.equals("ROLE_ADMINISTRATOR")) {
				admin = true;
			}
			if (name.equals("ROLE_INSTRUCTOR")) {
				instructor = true;
			}
		}

		return new LoginUser(id, admin, instructor);
	}

	private static UUID parseId(String subject) {
		if (subject == null || subject.isBlank()) {
			log.warn("chưa đăng nhập");
			return null;
		}
		try {
			return UUID.fromString(subject);
		}
		catch (IllegalArgumentException ex) {
			log.warn("Claim 'sub' không phải UUID hợp lệ: '{}' ", subject);
			return null;
		}
	}

	public boolean isGuest() {
		return id == null;
	}

	public boolean canManage(UUID ownerId) {
		if (id == null) {
			return false;
		}
		return instructor && id.equals(ownerId);
	}
}
