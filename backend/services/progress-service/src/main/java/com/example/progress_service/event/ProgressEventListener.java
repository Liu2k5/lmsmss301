package com.example.progress_service.event;

import com.example.progress_service.service.ProgressService;

import lombok.RequiredArgsConstructor;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProgressEventListener {

    private final ProgressService progressService;

    @RabbitListener(queues = "student.enrolled.queue")
    public void handleStudentEnrolled(StudentEnrolledEvent event) {

        System.out.println("================================");
        System.out.println("Received StudentEnrolledEvent");
        System.out.println("Event ID: " + event.getEventId());
        System.out.println("Student ID: " + event.getStudentId());
        System.out.println("Course ID: " + event.getCourseId());
        System.out.println("Lesson IDs: " + event.getLessonIds());
        System.out.println("================================");

        progressService.initializeProgress(event);
    }
}