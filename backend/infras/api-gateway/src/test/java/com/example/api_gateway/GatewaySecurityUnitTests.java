package com.example.api_gateway;

import com.example.api_gateway.security.*;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Unit tests only: principal is supplied, JWT signature and filter chain are not executed. */
class GatewaySecurityUnitTests {
    private static final String SUB = "11111111-1111-1111-1111-111111111111";
    @BeforeEach void before() { SecurityContextHolder.clearContext(); }
    @AfterEach void after() { SecurityContextHolder.clearContext(); }
    private Jwt jwt(Map<String, Object> claims) {
        var builder = Jwt.withTokenValue("unit-test").header("alg", "RS256").subject(SUB)
                .issuedAt(Instant.parse("2026-01-01T00:00:00Z"));
        claims.forEach(builder::claim); return builder.build();
    }
    private List<String> convert(Map<String, Object> claims) {
        return new KeycloakJwtAuthoritiesConverter().convert(jwt(claims)).stream().map(a -> a.getAuthority()).toList();
    }
    private void authenticate(String... authorities) {
        var roles = Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(jwt(Map.of()), null, roles));
    }
    private HttpServletRequest filter(MockHttpServletRequest request) throws Exception {
        var chain = new MockFilterChain();
        new UserHeaderFilter().doFilter(request, new MockHttpServletResponse(), chain);
        return (HttpServletRequest) chain.getRequest();
    }
    @Test void convertsRealmRolesWithPrefixAndUppercase() {
        assertEquals(List.of("ROLE_STUDENT", "ROLE_INSTRUCTOR", "ROLE_ADMIN"),
                convert(Map.of("realm_access", Map.of("roles", List.of("student", "instructor", "admin")))));
    }
    @Test void missingRealmClaimHasNoAuthorities() { assertTrue(convert(Map.of()).isEmpty()); }
    @Test void missingRolesHasNoAuthorities() { assertTrue(convert(Map.of("realm_access", Map.of())).isEmpty()); }
    @Test void emptyRolesHasNoAuthorities() { assertTrue(convert(Map.of("realm_access", Map.of("roles", List.of()))).isEmpty()); }
    @Test void stringRolesClaimIsIgnored() {
        assertTrue(convert(Map.of("realm_access", Map.of("roles", "admin"))).isEmpty());
    }
    @Test void malformedRealmClaimIsIgnored() {
        assertTrue(convert(Map.of("realm_access", "admin")).isEmpty());
    }
    @Test void malformedRoleElementsDoNotCreateAuthorities() {
        var roles = Arrays.asList(null, 42, true, "", " ", Map.of("name", "admin"), "student");
        assertEquals(List.of("ROLE_STUDENT"), convert(Map.of("realm_access", Map.of("roles", roles))));
    }
    @Test void roleConversionDoesNotDependOnDefaultLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals(List.of("ROLE_INSTRUCTOR"), convert(Map.of("realm_access", Map.of("roles", List.of("instructor")))));
        } finally { Locale.setDefault(original); }
    }
    @Test void replacesSpoofedHeadersInEveryAccessorAndPreservesOtherHeaders() throws Exception {
        authenticate("ROLE_STUDENT");
        var request = new MockHttpServletRequest("GET", "/api/v1/enrollments/me");
        request.addHeader("x-user-id", "attacker"); request.addHeader("x-user-id", "second-attacker");
        request.addHeader("X-USER-ROLE", "admin"); request.addHeader("X-USER-ROLE", "instructor");
        request.addHeader("X-Trace", "trace-1"); request.addHeader("X-Trace", "trace-2");
        HttpServletRequest wrapped = filter(request);
        assertEquals(SUB, wrapped.getHeader("X-USER-ID"));
        assertEquals("student", wrapped.getHeader("x-user-role"));
        assertEquals(List.of(SUB), Collections.list(wrapped.getHeaders("x-user-id")));
        assertEquals(List.of("student"), Collections.list(wrapped.getHeaders("X-USER-ROLE")));
        assertEquals(List.of("trace-1", "trace-2"), Collections.list(wrapped.getHeaders("X-Trace")));
        var names = Collections.list(wrapped.getHeaderNames());
        assertEquals(1, names.stream().filter(n -> n.equalsIgnoreCase("X-User-Id")).count());
        assertEquals(1, names.stream().filter(n -> n.equalsIgnoreCase("X-User-Role")).count());
        assertEquals("trace-1", wrapped.getHeader("X-Trace")); assertNull(wrapped.getHeader("missing"));
    }
    @ParameterizedTest
    @CsvSource({"ROLE_ADMIN;ROLE_STUDENT;ROLE_INSTRUCTOR,admin", "ROLE_STUDENT;ROLE_INSTRUCTOR,instructor", "ROLE_STUDENT,student", "SCOPE_read,''"})
    void selectsHighestKnownRoleRegardlessOfAuthorityOrder(String authorities, String expected) throws Exception {
        authenticate(authorities.split(";"));
        assertEquals(expected, filter(new MockHttpServletRequest()).getHeader("X-User-Role"));
    }
    @Test void noAuthoritiesDoNotReuseSpoofedRole() throws Exception {
        authenticate(); var req = new MockHttpServletRequest(); req.addHeader("X-User-Role", "admin");
        assertEquals("", filter(req).getHeader("X-User-Role"));
    }
    @Test void jwtAddsIdentityEvenIfClientSuppliesNoHeaders() throws Exception {
        authenticate("ROLE_ADMIN"); var wrapped = filter(new MockHttpServletRequest());
        assertEquals(SUB, wrapped.getHeader("X-User-Id"));
        assertEquals(List.of("admin"), Collections.list(wrapped.getHeaders("X-User-Role")));
    }
    @Test void unauthenticatedRequestIsLeftForSecurityChainToReject() throws Exception {
        var req = new MockHttpServletRequest();
        assertSame(req, filter(req)); assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
    @Test void nonJwtPrincipalIsNotConvertedToTrustedHeaders() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("not-jwt", null, List.of()));
        var req = new MockHttpServletRequest(); assertSame(req, filter(req));
        assertNull(req.getHeader("X-User-Id"));
    }
}
