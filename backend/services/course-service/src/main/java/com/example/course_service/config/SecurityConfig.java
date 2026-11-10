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
						// Danh mục khoá học (UC-C25…C27): ai cũng xem được, chỉ Admin được tạo / sửa / xoá.
						.requestMatchers(HttpMethod.GET, "/api/v1/categories", "/api/v1/categories/**").permitAll()
						.requestMatchers("/api/v1/categories", "/api/v1/categories/**")
						.hasAnyRole("ADMIN", "ADMINISTRATOR")
						// Soạn nội dung khoá học (UC-C01…C13): chỉ giảng viên. Admin quản lý hệ thống, không soạn khoá học.
						.requestMatchers(HttpMethod.POST,
								"/api/v1/courses/**", "/api/v1/sections/**", "/api/v1/lessons/**",
								"/api/v1/materials/**")
						.hasRole("INSTRUCTOR")
						.requestMatchers(HttpMethod.PUT,
								"/api/v1/courses/**", "/api/v1/sections/**", "/api/v1/lessons/**",
								"/api/v1/materials/**")
						.hasRole("INSTRUCTOR")
						.requestMatchers(HttpMethod.PATCH, "/api/v1/courses/**")
						.hasRole("INSTRUCTOR")
						.requestMatchers(HttpMethod.DELETE,
								"/api/v1/courses/**", "/api/v1/sections/**", "/api/v1/lessons/**",
								"/api/v1/materials/**")
						.hasRole("INSTRUCTOR")
						.requestMatchers(HttpMethod.POST, "/api/v1/quizzes/**").hasRole("INSTRUCTOR")
						.requestMatchers(HttpMethod.PUT, "/api/v1/quizzes/**").hasRole("INSTRUCTOR")
						.requestMatchers(HttpMethod.DELETE, "/api/v1/quizzes/**").hasRole("INSTRUCTOR")
						.requestMatchers(HttpMethod.GET, "/api/v1/quizzes/*").hasRole("INSTRUCTOR")
						.requestMatchers("/api/v1/quizzes/**").authenticated()
						// "Khoá học của tôi" (gồm cả bản nháp): chỉ giảng viên.
						.requestMatchers("/api/v1/instructor/**").hasRole("INSTRUCTOR")
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
