package com.example.enrollment_service;

import com.example.enrollment_service.config.CacheConfig;
import com.example.enrollment_service.domain.*;
import com.example.enrollment_service.security.HeaderAuthFilter;
import com.example.enrollment_service.service.EnrollmentService;
import com.example.enrollment_service.web.EnrollmentController;
import com.example.enrollment_service.web.dto.*;
import com.example.enrollment_service.web.exception.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.cache.Cache;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Pure unit tests. Direct controller calls do not execute MVC/security/validation. */
class SupportingUnitTests {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    @BeforeEach void clearBefore() { SecurityContextHolder.clearContext(); }
    @AfterEach void clearAfter() { SecurityContextHolder.clearContext(); }

    @ParameterizedTest @EnumSource(Status.class)
    void checkResponseAllowsOnlyActive(Status status) {
        Enrollment e = new Enrollment(); e.setStatus(status); e.setAccessGranted(true);
        CheckResponse r = CheckResponse.of(e, NOW);
        assertTrue(r.enrolled()); assertEquals(status.name(), r.status());
        assertEquals(status == Status.ACTIVE, r.accessGranted());
    }
    @ParameterizedTest @ValueSource(longs = {-1, 0, 1})
    void expiryBoundaryIsStrictlyAfterNow(long offset) {
        Enrollment e = new Enrollment(); e.setStatus(Status.ACTIVE); e.setAccessGranted(true);
        e.setExpiresAt(NOW.plusSeconds(offset));
        CheckResponse r = CheckResponse.of(e, NOW);
        assertEquals(offset > 0, r.accessGranted()); assertEquals(e.getExpiresAt(), r.expiresAt());
    }
    @Test void accessFlagFalseDeniesEvenBeforeExpiry() {
        Enrollment e = new Enrollment(); e.setStatus(Status.ACTIVE); e.setExpiresAt(NOW.plusSeconds(1));
        assertFalse(CheckResponse.of(e, NOW).accessGranted());
    }
    @Test void noneResponseHasNoAccessOrExpiry() {
        assertEquals(new CheckResponse(false, "NONE", false, null), CheckResponse.none());
    }
    @Test void completeHeadersCreateAuthenticationAndContinueChain() throws Exception {
        var req = new MockHttpServletRequest(); req.addHeader("X-User-Id", "user"); req.addHeader("X-User-Role", "student");
        var chain = new MockFilterChain();
        new HeaderAuthFilter().doFilter(req, new MockHttpServletResponse(), chain);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth); assertTrue(auth.isAuthenticated()); assertEquals("user", auth.getPrincipal());
        assertEquals(List.of("ROLE_STUDENT"), auth.getAuthorities().stream().map(a -> a.getAuthority()).toList());
        assertSame(req, chain.getRequest());
    }
    @ParameterizedTest @CsvSource(value = {"NULL,student", "user,NULL", "'',student", "user,''", "' ',student", "user,' '"}, nullValues = "NULL")
    void missingOrBlankHeadersDoNotAuthenticate(String user, String role) throws Exception {
        var req = new MockHttpServletRequest();
        if (user != null) req.addHeader("X-User-Id", user);
        if (role != null) req.addHeader("X-User-Role", role);
        var chain = new MockFilterChain();
        new HeaderAuthFilter().doFilter(req, new MockHttpServletResponse(), chain);
        assertNull(SecurityContextHolder.getContext().getAuthentication()); assertSame(req, chain.getRequest());
    }
    @Test void allControllerMethodsDelegateToServiceWithExactIdentifiers() {
        EnrollmentService service = mock(EnrollmentService.class);
        var controller = new EnrollmentController(service);
        UUID student = UUID.randomUUID(), course = UUID.randomUUID();
        var response = new EnrollmentResponse(UUID.randomUUID(), student, course, "ACTIVE", NOW, null, null, true);
        var check = new CheckResponse(true, "ACTIVE", true, null);
        var list = List.of(response);
        when(service.enroll(student, course)).thenReturn(response);
        when(service.myEnrollments(student)).thenReturn(list);
        when(service.activeStudentsOfCourse(course)).thenReturn(list);
        when(service.check(student, course)).thenReturn(check);
        when(service.grant(student, course)).thenReturn(response);
        assertSame(response, controller.enroll(student, new EnrollRequest(course)));
        controller.cancel(student, course);
        assertSame(list, controller.me(student)); assertSame(list, controller.students(course));
        assertSame(check, controller.check(student, course));
        assertSame(response, controller.grant(new TargetRequest(student, course)));
        controller.revoke(new TargetRequest(student, course));
        verify(service).enroll(student, course); verify(service).cancel(student, course);
        verify(service).myEnrollments(student); verify(service).activeStudentsOfCourse(course);
        verify(service).check(student, course); verify(service).grant(student, course); verify(service).revoke(student, course);
        verifyNoMoreInteractions(service);
    }
    @Test void controllerDoesNotSwallowServiceExceptions() {
        EnrollmentService service = mock(EnrollmentService.class);
        UUID student = UUID.randomUUID(), course = UUID.randomUUID();
        var failure = new ConflictException("duplicate");
        when(service.enroll(student, course)).thenThrow(failure);
        assertSame(failure, assertThrows(ConflictException.class,
                () -> new EnrollmentController(service).enroll(student, new EnrollRequest(course))));
    }
    @Test void adviceMapsNotFoundAndConflictToErrorEnvelope() {
        var advice = new GlobalExceptionHandler();
        var notFound = advice.notFound(new NotFoundException("missing"));
        assertEquals(404, notFound.getStatusCode().value());
        assertEquals(new ErrorResponse("NOT_FOUND", "missing", null), notFound.getBody());
        var conflict = advice.conflict(new ConflictException("duplicate"));
        assertEquals(409, conflict.getStatusCode().value());
        assertEquals(new ErrorResponse("CONFLICT", "duplicate", null), conflict.getBody());
    }
    @Test void adviceCollectsValidationFieldsKeepingFirstDuplicate() {
        var binding = new BeanPropertyBindingResult(new Object(), "request");
        binding.addError(new FieldError("request", "courseId", "required"));
        binding.addError(new FieldError("request", "courseId", "second"));
        var ex = mock(MethodArgumentNotValidException.class); when(ex.getBindingResult()).thenReturn(binding);
        var result = new GlobalExceptionHandler().validation(ex);
        assertEquals(400, result.getStatusCode().value());
        assertEquals(new ErrorResponse("VALIDATION_ERROR", "Invalid request", Map.of("courseId", "required")), result.getBody());
    }
    @Test void allCacheFailureCallbacksSwallowInfrastructureException() {
        Cache cache = mock(Cache.class); when(cache.getName()).thenReturn("enrollment-check");
        var handler = new CacheConfig().errorHandler(); var failure = new IllegalStateException("offline");
        assertDoesNotThrow(() -> handler.handleCacheGetError(failure, cache, "key"));
        assertDoesNotThrow(() -> handler.handleCachePutError(failure, cache, "key", "value"));
        assertDoesNotThrow(() -> handler.handleCacheEvictError(failure, cache, "key"));
        assertDoesNotThrow(() -> handler.handleCacheClearError(failure, cache));
    }
}
