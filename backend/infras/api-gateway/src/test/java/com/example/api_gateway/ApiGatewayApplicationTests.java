package com.example.api_gateway;

import com.example.api_gateway.security.UserHeaderFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiGatewayApplicationTests {

	private static final String SUB = "11111111-1111-1111-1111-111111111111";

	@AfterEach
	void clear() {
		SecurityContextHolder.clearContext();
	}

	private static Jwt jwt() {
		return Jwt.withTokenValue("t").header("alg", "RS256").subject(SUB)
				.issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
	}

	private static void authenticate(String... roles) {
		List<SimpleGrantedAuthority> authorities = java.util.Arrays.stream(roles)
				.map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList();
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(jwt(), null, authorities));
	}

	private static HttpServletRequest runFilter(MockHttpServletRequest request) throws Exception {
		MockFilterChain chain = new MockFilterChain();
		new UserHeaderFilter().doFilter(request, new MockHttpServletResponse(), chain);
		return (HttpServletRequest) chain.getRequest();
	}

	@Test
	void replacesClientSuppliedHeadersWithJwtValues() throws Exception {
		authenticate("STUDENT");
		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/enrollments/me");
		req.addHeader("X-User-Id", "33333333-3333-3333-3333-333333333333");
		req.addHeader("X-User-Role", "admin");

		HttpServletRequest out = runFilter(req);

		assertEquals(SUB, out.getHeader("X-User-Id"));
		assertEquals("student", out.getHeader("X-User-Role"));
		assertEquals(SUB, out.getHeaders("X-User-Id").nextElement());
	}

	@Test
	void highestPrivilegeRoleWins() throws Exception {
		authenticate("STUDENT", "INSTRUCTOR");

		HttpServletRequest out = runFilter(new MockHttpServletRequest());

		assertEquals("instructor", out.getHeader("X-User-Role"));
	}
}
