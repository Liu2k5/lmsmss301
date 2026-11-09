package com.example.course_service.config;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.example.course_service.security.KeycloakRoleConverter;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Catalog (khoá học / chương / bài học / học liệu) vẫn công khai để khách xem.
 * Riêng quiz bị hạn chế theo bảng use case UC-C14…C19:
 * <ul>
 *   <li>Tạo / sửa / xoá quiz, câu hỏi, nhân bản, sắp xếp: chỉ INSTRUCTOR và ADMIN</li>
 *   <li>Xem chi tiết quiz (kèm đáp án đúng) và danh sách bài học để gắn quiz: chỉ INSTRUCTOR và ADMIN</li>
 *   <li>Liệt kê quiz và bản xem trước (không có đáp án): mọi người dùng đã đăng nhập</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

	@Bean
	JwtDecoder jwtDecoder(
			@Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri) {
		return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, KeycloakRoleConverter roleConverter)
			throws Exception {
		JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
		jwtConverter.setJwtGrantedAuthoritiesConverter(roleConverter);

		http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
						// Sửa nội dung quiz (UC-C14…C19): chỉ giảng viên soạn được quiz.
						.requestMatchers(HttpMethod.POST, "/api/v1/quizzes/**").hasAnyRole("INSTRUCTOR", "ADMIN")
						.requestMatchers(HttpMethod.PUT, "/api/v1/quizzes/**").hasAnyRole("INSTRUCTOR", "ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/api/v1/quizzes/**").hasAnyRole("INSTRUCTOR", "ADMIN")
						// Chi tiết quiz trả về đáp án đúng; danh sách bài học để gắn quiz cũng là công cụ soạn bài.
						.requestMatchers(HttpMethod.GET, "/api/v1/quizzes/*").hasAnyRole("INSTRUCTOR", "ADMIN")
						// Danh sách quiz và bản xem trước (không có đáp án): cần đăng nhập.
						.requestMatchers("/api/v1/quizzes/**").authenticated()
						// Phần còn lại giữ nguyên như trước khi nối xác thực.
						.anyRequest().permitAll())
				.oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter)))
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint((request, response, error) ->
								writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
										"Bạn cần đăng nhập để thực hiện thao tác này"))
						.accessDeniedHandler((request, response, error) ->
								writeError(response, HttpServletResponse.SC_FORBIDDEN,
										"Bạn không có quyền thực hiện thao tác này")));
		return http.build();
	}

	private static void writeError(HttpServletResponse response, int status, String message) throws IOException {
		response.setStatus(status);
		response.setContentType("application/json;charset=UTF-8");
		response.getWriter().write("{\"status\":" + status + ",\"message\":\"" + message + "\",\"details\":[]}");
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://127.0.0.1:5173"));
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
