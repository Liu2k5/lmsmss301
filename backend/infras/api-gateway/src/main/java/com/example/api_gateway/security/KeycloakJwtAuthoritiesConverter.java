package com.example.api_gateway.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Maps Keycloak realm roles (realm_access.roles) to ROLE_* authorities. */
@Component
public class KeycloakJwtAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

	@Override
	public Collection<GrantedAuthority> convert(Jwt jwt) {
		List<GrantedAuthority> authorities = new ArrayList<>();
		Object claim = jwt.getClaims().get("realm_access");
		if (claim instanceof Map<?, ?> realmAccess && realmAccess.get("roles") instanceof List<?> roles) {
			for (Object role : roles) {
				if (role instanceof String name && !name.isBlank()) {
					authorities.add(new SimpleGrantedAuthority("ROLE_" + name.toUpperCase(Locale.ROOT)));
				}
			}
		}
		return authorities;
	}
}
