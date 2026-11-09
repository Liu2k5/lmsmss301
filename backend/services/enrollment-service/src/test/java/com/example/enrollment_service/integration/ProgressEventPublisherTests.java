package com.example.enrollment_service.integration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import tools.jackson.databind.ObjectMapper;

class ProgressEventPublisherTests {

    private RabbitTemplate rabbitTemplate;
    private CourseCatalogClient courseCatalog;
    private ObjectMapper objectMapper;
    private ProgressEventPublisher publisher;

    private final UUID enrollmentId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID studentId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private final UUID courseId = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        courseCatalog = mock(CourseCatalogClient.class);
        objectMapper = new ObjectMapper();
        publisher = new ProgressEventPublisher(rabbitTemplate, courseCatalog, objectMapper);
    }

    @Test
    void onEnrollmentActivated_sendsToStudentEnrolledQueue() {
        UUID lesson1 = UUID.randomUUID();
        when(courseCatalog.publishedLessonIds(courseId)).thenReturn(List.of(lesson1));

        publisher.onEnrollmentActivated(new EnrollmentActivatedEvent(enrollmentId, studentId, courseId));

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(
                eq(ProgressEventPublisher.STUDENT_ENROLLED_QUEUE),
                payloadCaptor.capture(),
                any(MessagePostProcessor.class));

        String payload = (String) payloadCaptor.getValue();
        assertTrue(payload.contains(enrollmentId.toString()));
        assertTrue(payload.contains(studentId.toString()));
        assertTrue(payload.contains(courseId.toString()));
        assertTrue(payload.contains(lesson1.toString()));
    }

    @Test
    void onEnrollmentActivated_whenNoPublishedLessons_doesNotSend() {
        when(courseCatalog.publishedLessonIds(courseId)).thenReturn(List.of());

        publisher.onEnrollmentActivated(new EnrollmentActivatedEvent(enrollmentId, studentId, courseId));

        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void onEnrollmentDeactivated_sendsToStudentUnenrolledQueue() {
        UUID eventId = UUID.randomUUID();
        publisher.onEnrollmentDeactivated(new EnrollmentDeactivatedEvent(eventId, studentId, courseId));

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(
                eq(ProgressEventPublisher.STUDENT_UNENROLLED_QUEUE),
                payloadCaptor.capture(),
                any(MessagePostProcessor.class));

        String payload = (String) payloadCaptor.getValue();
        assertTrue(payload.contains(eventId.toString()));
        assertTrue(payload.contains(studentId.toString()));
        assertTrue(payload.contains(courseId.toString()));
    }

    @Test
    void onEnrollmentDeactivated_whenBrokerFails_doesNotThrow() {
        UUID eventId = UUID.randomUUID();
        doThrow(new RuntimeException("RabbitMQ connection refused"))
                .when(rabbitTemplate)
                .convertAndSend(
                        eq(ProgressEventPublisher.STUDENT_UNENROLLED_QUEUE),
                        any(Object.class),
                        any(MessagePostProcessor.class)
                );

        assertDoesNotThrow(() ->
                publisher.onEnrollmentDeactivated(new EnrollmentDeactivatedEvent(eventId, studentId, courseId))
        );
    }
}
