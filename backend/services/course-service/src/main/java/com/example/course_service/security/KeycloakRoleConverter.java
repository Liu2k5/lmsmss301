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

@Component
public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

	@Override
	public Collection<GrantedAuthority> convert(Jwt jwt) {
		List<GrantedAuthority> authorities = new ArrayList<>();

		Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
		if (realmAccess == null) {
			return authorities;
		}

		Object rolesObject = realmAccess.get("roles");
		if (rolesObject == null) {
			return authorities;
		}
		if (!(rolesObject instanceof List)) {
			return authorities;
		}
		List<?> roles = (List<?>) rolesObject;

		for (Object role : roles) {

			if (role == null) {
				continue;
			}

			if (!(role instanceof String)) {
				continue;
			}

			String roleName = (String) role;

			if (roleName.isBlank()) {
				continue;
			}

			String upperRoleName = roleName.toUpperCase(Locale.ROOT);

			String authorityName = "ROLE_" + upperRoleName;

			GrantedAuthority authority = new SimpleGrantedAuthority(authorityName);
			authorities.add(authority);
		}

		return authorities;
	}
}

