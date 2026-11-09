package com.example.course_service.security;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** Người dùng đang gọi API, lấy từ JWT mà Gateway (hoặc service) đã xác thực. */
@Component
public class CurrentUser {

	/** UUID trong JWT `sub` — cũng là instructorId khi giảng viên tạo khoá học. */
	public Optional<UUID> id() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
			return Optional.empty();
		}
		try {
			return Optional.of(UUID.fromString(jwt.getSubject()));
		}
		catch (RuntimeException ex) {
			return Optional.empty();
		}
	}

	public boolean isAdmin() {
		return hasRole("ROLE_ADMIN") || hasRole("ROLE_ADMINISTRATOR");
	}

	private boolean hasRole(String role) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null) {
			return false;
		}
		for (GrantedAuthority authority : auth.getAuthorities()) {
			if (role.equals(authority.getAuthority())) {
				return true;
			}
		}
		return false;
	}
}
