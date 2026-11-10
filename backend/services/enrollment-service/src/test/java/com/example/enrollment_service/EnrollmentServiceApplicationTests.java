package com.example.enrollment_service;

import com.example.enrollment_service.domain.*;
import com.example.enrollment_service.integration.EnrollmentActivatedEvent;
import com.example.enrollment_service.integration.EnrollmentCancelledEvent;
import com.example.enrollment_service.repository.*;
import com.example.enrollment_service.service.EnrollmentService;
import com.example.enrollment_service.web.dto.EnrollmentResponse;
import com.example.enrollment_service.web.exception.ConflictException;
import com.example.enrollment_service.web.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Isolated service tests: no Spring proxies, database, Redis, or broker. */
class EnrollmentServiceApplicationTests {
    private final UUID student = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID course = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private final Instant originalTime = Instant.parse("2025-01-01T00:00:00Z");
    private EnrollmentRepository enrollments;
    private EnrollmentAccessRepository accesses;
    private EnrollEventRepository events;
    private ApplicationEventPublisher applicationEvents;
    private EnrollmentService service;

    @BeforeEach
    void setUp() {
        enrollments = mock(EnrollmentRepository.class);
        accesses = mock(EnrollmentAccessRepository.class);
        events = mock(EnrollEventRepository.class);
        applicationEvents = mock(ApplicationEventPublisher.class);
        service = new EnrollmentService(enrollments, accesses, events, applicationEvents);
    }

    private Enrollment existing(Status status) {
        Enrollment e = new Enrollment();
        e.setId(UUID.randomUUID());
        e.setStudentId(student);
        e.setCourseId(course);
        e.setStatus(status);
        e.setAccessGranted(status == Status.ACTIVE);
        e.setEnrolledAt(originalTime);
        e.setCreatedAt(originalTime);
        e.setUpdatedAt(originalTime);
        when(enrollments.findByStudentIdAndCourseIdAndDeletedAtIsNull(student, course))
                .thenReturn(Optional.of(e));
        return e;
    }

    private Enrollment savedEnrollment() {
        var captor = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollments).save(captor.capture());
        return captor.getValue();
    }

    private void assertActivationPublished(Enrollment e) {
        var captor = ArgumentCaptor.forClass(EnrollmentActivatedEvent.class);
        verify(applicationEvents, atLeastOnce()).publishEvent(captor.capture());
        EnrollmentActivatedEvent published = captor.getValue();
        assertNotNull(published.eventId(), "eventId phải là id của bản ghi enroll_events");
        assertEquals(e.getId(), published.enrollmentId());
        assertEquals(student, published.studentId());
        assertEquals(course, published.courseId());
    }

    private void assertAccessAndEvent(Enrollment e, AccessStatus accessStatus, EventType eventType) {
        var accessCaptor = ArgumentCaptor.forClass(EnrollmentAccess.class);
        verify(accesses).save(accessCaptor.capture());
        EnrollmentAccess access = accessCaptor.getValue();
        assertNotNull(access.getId());
        assertEquals(e.getId(), access.getEnrollmentId());
        assertEquals(AccessType.STANDARD, access.getAccessType());
        assertEquals(accessStatus, access.getStatus());
        assertNotNull(access.getGrantedAt());
        assertEquals(eventType.name(), access.getReason());
        if (accessStatus == AccessStatus.REVOKED) {
            assertEquals(e.getUpdatedAt(), access.getRevokedAt());
        } else {
            assertNull(access.getRevokedAt());
        }
        var eventCaptor = ArgumentCaptor.forClass(EnrollEvent.class);
        verify(events).save(eventCaptor.capture());
        EnrollEvent event = eventCaptor.getValue();
        assertNotNull(event.getId());
        assertEquals(e.getId(), event.getEnrollmentId());
        assertEquals(eventType, event.getEventType());
        assertEquals(e.getUpdatedAt(), event.getCreatedAt());
    }

    @Test
    void enrollCreatesActiveEnrollmentAndLinkedAccessAndEvent() {
        Instant before = Instant.now();
        EnrollmentResponse response = service.enroll(student, course);
        Instant after = Instant.now();
        Enrollment e = savedEnrollment();
        assertNotNull(e.getId());
        assertEquals(student, e.getStudentId());
        assertEquals(course, e.getCourseId());
        assertEquals(Status.ACTIVE, e.getStatus());
        assertTrue(e.isAccessGranted());
        assertNull(e.getCancelledAt());
        assertNull(e.getDeletedAt());
        assertEquals(e.getCreatedAt(), e.getEnrolledAt());
        assertEquals(e.getCreatedAt(), e.getUpdatedAt());
        assertFalse(e.getCreatedAt().isBefore(before));
        assertFalse(e.getCreatedAt().isAfter(after));
        assertEquals(EnrollmentResponse.from(e), response);
        assertAccessAndEvent(e, AccessStatus.GRANTED, EventType.ENROLLED);
        assertActivationPublished(e);
    }

    @Test
    void grantPublishesActivationForProgressService() {
        Enrollment e = existing(Status.SUSPENDED);
        service.grant(student, course);

        assertActivationPublished(e);
    }

    @Test
    void enrollReactivatesCancelledEnrollmentWithoutChangingIdentity() {
        Enrollment e = existing(Status.CANCELLED);
        e.setCancelledAt(originalTime);
        UUID id = e.getId();
        service.enroll(student, course);
        assertSame(e, savedEnrollment());
        assertEquals(id, e.getId());
        assertEquals(originalTime, e.getCreatedAt());
        assertEquals(originalTime, e.getEnrolledAt());
        assertNull(e.getCancelledAt());
        assertEquals(Status.ACTIVE, e.getStatus());
        assertTrue(e.isAccessGranted());
        assertAccessAndEvent(e, AccessStatus.GRANTED, EventType.ENROLLED);
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"ACTIVE", "SUSPENDED", "COMPLETED", "EXPIRED"})
    void studentCannotReactivateProtectedStates(Status status) {
        Enrollment e = existing(status);
        assertThrows(ConflictException.class, () -> service.enroll(student, course));
        assertEquals(status, e.getStatus());
        verify(enrollments, never()).save(any());
        verifyNoInteractions(accesses, events);
    }

    @Test
    void cancelActiveEnrollmentDisablesAccessAndRecordsCancellation() {
        Enrollment e = existing(Status.ACTIVE);
        service.cancel(student, course);
        assertSame(e, savedEnrollment());
        assertEquals(Status.CANCELLED, e.getStatus());
        assertFalse(e.isAccessGranted());
        assertNotNull(e.getCancelledAt());
        assertEquals(e.getCancelledAt(), e.getUpdatedAt());
        assertAccessAndEvent(e, AccessStatus.REVOKED, EventType.CANCELLED);
    }

    @Test
    void cancelPublishesUnenrolledEventForProgressArchive() {
        existing(Status.ACTIVE);
        service.cancel(student, course);

        var captor = ArgumentCaptor.forClass(EnrollmentCancelledEvent.class);
        verify(applicationEvents).publishEvent(captor.capture());
        EnrollmentCancelledEvent published = captor.getValue();
        assertNotNull(published.eventId(), "eventId phải là id của bản ghi enroll_events");
        assertEquals(student, published.studentId());
        assertEquals(course, published.courseId());
    }

    @Test
    void cancelAlreadyCancelledEnrollmentKeepsAccessDisabled() {
        Enrollment e = existing(Status.CANCELLED);
        service.cancel(student, course);
        assertSame(e, savedEnrollment());
        assertEquals(Status.CANCELLED, e.getStatus());
        assertFalse(e.isAccessGranted());
        assertAccessAndEvent(e, AccessStatus.REVOKED, EventType.CANCELLED);
    }

    @Test
    void cancelMissingEnrollmentHasNoWrites() {
        assertThrows(NotFoundException.class, () -> service.cancel(student, course));
        verify(enrollments, never()).save(any());
        verifyNoInteractions(accesses, events);
    }

    @Test
    void suspendedStudentCannotCancelThenReenrollToBypassAdminRevoke() {
        Enrollment e = existing(Status.SUSPENDED);
        assertThrows(ConflictException.class, () -> service.cancel(student, course));
        assertThrows(ConflictException.class, () -> service.enroll(student, course));
        assertEquals(Status.SUSPENDED, e.getStatus());
        verify(enrollments, never()).save(any());
        verifyNoInteractions(accesses, events);
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"COMPLETED", "EXPIRED"})
    void studentCannotCancelProtectedStates(Status status) {
        Enrollment e = existing(status);
        assertThrows(ConflictException.class, () -> service.cancel(student, course));
        assertEquals(status, e.getStatus());
        verify(enrollments, never()).save(any());
        verifyNoInteractions(accesses, events);
    }

    @Test
    void grantCreatesEnrollmentForAdminTarget() {
        EnrollmentResponse response = service.grant(student, course);
        Enrollment e = savedEnrollment();
        assertEquals(student, e.getStudentId());
        assertEquals(course, e.getCourseId());
        assertEquals("ACTIVE", response.status());
        assertTrue(response.accessGranted());
        assertAccessAndEvent(e, AccessStatus.GRANTED, EventType.GRANTED);
    }

    @ParameterizedTest
    @EnumSource(Status.class)
    void adminGrantCanActivateExistingEnrollment(Status status) {
        Enrollment e = existing(status);
        e.setCancelledAt(originalTime);
        service.grant(student, course);
        assertSame(e, savedEnrollment());
        assertEquals(Status.ACTIVE, e.getStatus());
        assertTrue(e.isAccessGranted());
        assertNull(e.getCancelledAt());
        assertEquals(originalTime, e.getCreatedAt());
        assertAccessAndEvent(e, AccessStatus.GRANTED, EventType.GRANTED);
    }

    @Test
    void revokeSuspendsEnrollmentAndRecordsRevocation() {
        Enrollment e = existing(Status.ACTIVE);
        service.revoke(student, course);
        assertSame(e, savedEnrollment());
        assertEquals(Status.SUSPENDED, e.getStatus());
        assertFalse(e.isAccessGranted());
        assertAccessAndEvent(e, AccessStatus.REVOKED, EventType.REVOKED);
    }

    @Test
    void adminCanRestoreSuspendedEnrollmentAfterStudentIsRejected() {
        Enrollment e = existing(Status.ACTIVE);
        service.revoke(student, course);
        assertThrows(ConflictException.class, () -> service.enroll(student, course));
        service.grant(student, course);
        assertEquals(Status.ACTIVE, e.getStatus());
        assertTrue(e.isAccessGranted());
        assertTrue(service.check(student, course).accessGranted());
        verify(enrollments, times(2)).save(e);
        var eventCaptor = ArgumentCaptor.forClass(EnrollEvent.class);
        verify(events, times(2)).save(eventCaptor.capture());
        assertEquals(List.of(EventType.REVOKED, EventType.GRANTED),
                eventCaptor.getAllValues().stream().map(EnrollEvent::getEventType).toList());
    }

    @Test
    void revokeMissingEnrollmentHasNoWrites() {
        assertThrows(NotFoundException.class, () -> service.revoke(student, course));
        verify(enrollments, never()).save(any());
        verifyNoInteractions(accesses, events);
    }

    @Test
    void checkMissingEnrollmentReturnsNoneWithoutWriting() {
        var response = service.check(student, course);
        assertFalse(response.enrolled());
        assertEquals("NONE", response.status());
        assertFalse(response.accessGranted());
        assertNull(response.expiresAt());
        verify(enrollments).findByStudentIdAndCourseIdAndDeletedAtIsNull(student, course);
        verify(enrollments, never()).save(any());
        verifyNoInteractions(accesses, events);
    }

    @ParameterizedTest
    @EnumSource(Status.class)
    void checkMapsEveryStatusWithoutWriting(Status status) {
        Enrollment e = existing(status);
        e.setAccessGranted(true);
        var response = service.check(student, course);
        assertTrue(response.enrolled());
        assertEquals(status.name(), response.status());
        assertEquals(status == Status.ACTIVE, response.accessGranted());
        verify(enrollments, never()).save(any());
        verifyNoInteractions(accesses, events);
    }

    @Test
    void checkExpiredActiveEnrollmentDeniesAccess() {
        Enrollment e = existing(Status.ACTIVE);
        e.setExpiresAt(originalTime);
        var response = service.check(student, course);
        assertFalse(response.accessGranted());
        assertEquals(originalTime, response.expiresAt());
    }

    @Test
    void checkActiveEnrollmentBeforeExpiryGrantsAccess() {
        Enrollment e = existing(Status.ACTIVE);
        e.setExpiresAt(Instant.parse("9999-12-31T00:00:00Z"));
        var response = service.check(student, course);
        assertTrue(response.accessGranted());
        assertEquals(e.getExpiresAt(), response.expiresAt());
    }

    @Test
    void checkActiveEnrollmentWithAccessDisabledDeniesAccess() {
        Enrollment e = existing(Status.ACTIVE);
        e.setAccessGranted(false);
        assertFalse(service.check(student, course).accessGranted());
    }

    @Test
    void myEnrollmentsMapsAllResponseFieldsAndQueriesCurrentStudent() {
        Enrollment e = existing(Status.CANCELLED);
        e.setCancelledAt(originalTime.plusSeconds(10));
        e.setExpiresAt(originalTime.plusSeconds(20));
        when(enrollments.findAllByStudentIdAndDeletedAtIsNull(student)).thenReturn(List.of(e));
        assertEquals(List.of(EnrollmentResponse.from(e)), service.myEnrollments(student));
        verify(enrollments).findAllByStudentIdAndDeletedAtIsNull(student);
        verifyNoInteractions(accesses, events);
    }

    @Test
    void myEnrollmentsReturnsEmptyList() {
        assertTrue(service.myEnrollments(student).isEmpty());
        verify(enrollments).findAllByStudentIdAndDeletedAtIsNull(student);
    }

    @Test
    void activeStudentsQueriesOnlyActiveEnrollmentsOfRequestedCourse() {
        Enrollment e = existing(Status.ACTIVE);
        when(enrollments.findAllByCourseIdAndStatus(course, Status.ACTIVE)).thenReturn(List.of(e));
        assertEquals(List.of(EnrollmentResponse.from(e)), service.activeStudentsOfCourse(course));
        verify(enrollments).findAllByCourseIdAndStatus(course, Status.ACTIVE);
        verifyNoInteractions(accesses, events);
    }

    @Test
    void activeStudentsReturnsEmptyList() {
        assertTrue(service.activeStudentsOfCourse(course).isEmpty());
        verify(enrollments).findAllByCourseIdAndStatus(course, Status.ACTIVE);
    }

    @Test
    void enrollmentSaveFailureDoesNotContinueWithAccessOrEventWrites() {
        var failure = new IllegalStateException("database unavailable");
        when(enrollments.save(any())).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class, () -> service.enroll(student, course)));
        verifyNoInteractions(accesses, events);
    }

    @Test
    void accessSaveFailureIsPropagatedAndEventIsNotWritten() {
        var failure = new IllegalStateException("access persistence failed");
        when(accesses.save(any())).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class, () -> service.enroll(student, course)));
        verifyNoInteractions(events);
    }

    @Test
    void eventSaveFailureIsPropagatedRatherThanReportingSuccess() {
        var failure = new IllegalStateException("event persistence failed");
        when(events.save(any())).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class, () -> service.enroll(student, course)));
    }
}
  