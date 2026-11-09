package com.example.course_service.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** Đổi realm role của Keycloak (student / instructor / admin) thành ROLE_STUDENT… */
@Component
public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

	@Override
	public Collection<GrantedAuthority> convert(Jwt jwt) {
		List<GrantedAuthority> authorities = new ArrayList<>();
		if (jwt.getClaims().get("realm_access") instanceof Map<?, ?> realmAccess
				&& realmAccess.get("roles") instanceof Collection<?> roles) {
			for (Object role : roles) {
				if (role instanceof String name && !name.isBlank()) {
					authorities.add(new SimpleGrantedAuthority("ROLE_" + name.toUpperCase(Locale.ROOT)));
				}
			}
		}
		return authorities;
	}
}
