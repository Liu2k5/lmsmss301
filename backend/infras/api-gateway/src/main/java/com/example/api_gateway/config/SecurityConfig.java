package com.example.api_gateway.config;

import com.example.api_gateway.security.KeycloakJwtAuthoritiesConverter;
import com.example.api_gateway.security.UserHeaderFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http,
											KeycloakJwtAuthoritiesConverter authoritiesConverter) throws Exception {
		JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
		jwtConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
		UserHeaderFilter userHeaderFilter = new UserHeaderFilter();

		http
				.csrf(csrf -> csrf.disable())
				.cors(cors -> cors.configurationSource(corsSource()))
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health").permitAll()
						// Internal endpoint (D7): service-to-service only, never exposed through the gateway.
						.requestMatchers(HttpMethod.GET, "/api/v1/enrollments/check").denyAll()
						// Các endpoint công khai, khớp với cấu hình của từng service phía sau.
						.requestMatchers(HttpMethod.POST, "/api/v1/users/register").permitAll()
						.requestMatchers(HttpMethod.POST,
								"/api/v1/users/password/forgot",
								"/api/v1/users/password/reset").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/universities/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/courses/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/courses/*/reviews/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/courses/*/ratings/**").permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter)))
				.addFilterAfter(userHeaderFilter, BasicAuthenticationFilter.class)
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint((req, res, e) -> writeError(res, 401, "UNAUTHORIZED", "Authentication required"))
						.accessDeniedHandler((req, res, e) -> writeError(res, 403, "FORBIDDEN", "Access denied")));
		return http.build();
	}

	private static UrlBasedCorsConfigurationSource corsSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	private static void writeError(HttpServletResponse res, int status, String code, String message)
			throws IOException {
		res.setStatus(status);
		res.setContentType("application/json");
		res.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\",\"details\":null}");
	}
}
