package com.example.progress_service.event;

import com.example.progress_service.event.CurriculumChangedEvent;
import com.example.progress_service.service.CurriculumUpdateService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CurriculumChangedListener {

    private final CurriculumUpdateService curriculumUpdateService;

    @RabbitListener(queues = "curriculum.changed.queue")
    public void handleCurriculumChanged(CurriculumChangedEvent event) {

        log.info(
                "Received curriculum update: eventId={}, courseId={}",
                event.getEventId(),
                event.getCourseId()
        );

        curriculumUpdateService.handleCurriculumChanged(event);

        log.info(
                "Curriculum update processed: eventId={}",
                event.getEventId()
        );
    }
}