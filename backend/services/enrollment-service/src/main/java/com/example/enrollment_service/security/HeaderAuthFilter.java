package com.example.enrollment_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Trusts X-User-Id / X-User-Role set by the API Gateway (which strips client-supplied values).
 * Not a @Component: it is added only to the security chain.
 */
public class HeaderAuthFilter extends OncePerRequestFilter {

	static final String HEADER_USER_ID = "X-User-Id";
	static final String HEADER_USER_ROLE = "X-User-Role";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String userId = request.getHeader(HEADER_USER_ID);
		String role = request.getHeader(HEADER_USER_ROLE);
		if (userId != null && !userId.isBlank() && role != null && !role.isBlank()) {
			var authority = new SimpleGrantedAuthority("ROLE_" + role.toUpperCase());
			SecurityContextHolder.getContext().setAuthentication(
					new UsernamePasswordAuthenticationToken(userId, null, List.of(authority)));
		}
		chain.doFilter(request, response);
	}
}
