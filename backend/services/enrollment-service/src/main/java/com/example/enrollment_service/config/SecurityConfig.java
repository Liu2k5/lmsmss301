package com.example.enrollment_service.config;

import com.example.enrollment_service.security.HeaderAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.addFilterBefore(new HeaderAuthFilter(), UsernamePasswordAuthenticationFilter.class)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health").permitAll()
						// Internal service-to-service endpoint (D7); the gateway does not route it.
						.requestMatchers(HttpMethod.GET, "/api/v1/enrollments/check").permitAll()
						.requestMatchers("/api/v1/enrollments/admin/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.GET, "/api/v1/enrollments/courses/*/students")
						.hasAnyRole("INSTRUCTOR", "ADMIN")
						.requestMatchers(HttpMethod.POST, "/api/v1/enrollments").hasRole("STUDENT")
						.requestMatchers(HttpMethod.DELETE, "/api/v1/enrollments/*").hasRole("STUDENT")
						.anyRequest().authenticated())
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint((req, res, e) -> writeError(res, 401, "UNAUTHORIZED", "Authentication required"))
						.accessDeniedHandler((req, res, e) -> writeError(res, 403, "FORBIDDEN", "Access denied")));
		return http.build();
	}

	private static void writeError(jakarta.servlet.http.HttpServletResponse res, int status, String code, String message)
			throws java.io.IOException {
		res.setStatus(status);
		res.setContentType("application/json");
		res.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\",\"details\":null}");
	}
}
