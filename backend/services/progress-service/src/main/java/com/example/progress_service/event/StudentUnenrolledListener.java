
package com.example.progress_service.event;

import com.example.progress_service.service.ArchiveProgressService;

import lombok.RequiredArgsConstructor;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StudentUnenrolledListener {

    private final ArchiveProgressService archiveProgressService;

    @RabbitListener(
            queues = "student.unenrolled.queue"
    )
    public void handleStudentUnenrolled(
            StudentUnenrolledEvent event) {

        archiveProgressService.archiveProgress(event);
    }
}
