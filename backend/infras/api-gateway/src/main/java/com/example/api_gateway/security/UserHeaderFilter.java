package com.example.api_gateway.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Runs after authentication. Strips any client-supplied X-User-* headers and injects the
 * trusted values derived from the verified JWT, so downstream services can trust them.
 * Not a @Component: it is added only to the security chain, so it does not also run as a servlet filter.
 */
public class UserHeaderFilter extends OncePerRequestFilter {

	static final String HEADER_USER_ID = "X-User-Id";
	static final String HEADER_USER_ROLE = "X-User-Role";
	/** Highest privilege first. */
	private static final List<String> ROLE_PRIORITY = List.of("admin", "instructor", "student");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
			chain.doFilter(request, response);
			return;
		}
		String userId = jwt.getSubject();
		String role = highestRole(auth.getAuthorities());
		chain.doFilter(new TrustedHeaderRequest(request, userId, role), response);
	}

	private static String highestRole(Iterable<? extends GrantedAuthority> authorities) {
		Set<String> present = new LinkedHashSet<>();
		for (GrantedAuthority a : authorities) {
			present.add(a.getAuthority().replaceFirst("^ROLE_", "").toLowerCase());
		}
		for (String candidate : ROLE_PRIORITY) {
			if (present.contains(candidate)) {
				return candidate;
			}
		}
		return "";
	}

	private static final class TrustedHeaderRequest extends HttpServletRequestWrapper {

		private final String userId;
		private final String role;

		TrustedHeaderRequest(HttpServletRequest request, String userId, String role) {
			super(request);
			this.userId = userId;
			this.role = role;
		}

		private static boolean isReserved(String name) {
			return HEADER_USER_ID.equalsIgnoreCase(name) || HEADER_USER_ROLE.equalsIgnoreCase(name);
		}

		@Override
		public String getHeader(String name) {
			if (HEADER_USER_ID.equalsIgnoreCase(name)) {
				return userId;
			}
			if (HEADER_USER_ROLE.equalsIgnoreCase(name)) {
				return role;
			}
			return super.getHeader(name);
		}

		@Override
		public Enumeration<String> getHeaders(String name) {
			if (HEADER_USER_ID.equalsIgnoreCase(name)) {
				return Collections.enumeration(List.of(userId));
			}
			if (HEADER_USER_ROLE.equalsIgnoreCase(name)) {
				return Collections.enumeration(List.of(role));
			}
			return super.getHeaders(name);
		}

		@Override
		public Enumeration<String> getHeaderNames() {
			Set<String> names = new LinkedHashSet<>();
			Enumeration<String> original = super.getHeaderNames();
			while (original.hasMoreElements()) {
				String n = original.nextElement();
				if (!isReserved(n)) {
					names.add(n);
				}
			}
			names.add(HEADER_USER_ID);
			names.add(HEADER_USER_ROLE);
			return Collections.enumeration(names);
		}
	}
}
