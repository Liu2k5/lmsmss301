package com.example.progress_service.event;

import com.example.progress_service.service.QuizProgressService;

import lombok.RequiredArgsConstructor;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuizEventListener {

    private final QuizProgressService quizProgressService;

    @RabbitListener(queues = "quiz.submitted.queue")
    public void handleQuizSubmitted(
            QuizSubmittedEvent event) {

        quizProgressService.recordQuizResult(event);
    }
}