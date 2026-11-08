package com.example.progress_service.config;


import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Chuyển JSON từ RabbitMQ thành Java object
    @Bean
    public MessageConverter jacksonJsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public Queue studentEnrolledQueue() {

        return new Queue(
                "student.enrolled.queue",
                true
        );
    }

    @Bean
    public Queue quizSubmittedQueue() {
        return new Queue("quiz.submitted.queue", true);
    }

    public static final String CURRICULUM_CHANGED_QUEUE =
            "curriculum.changed.queue";

    @Bean
    public Queue curriculumChangedQueue() {
        return new Queue(CURRICULUM_CHANGED_QUEUE, true);
    }

    @Bean
    public Queue studentUnenrolledQueue() {
        return new Queue("student.unenrolled.queue", true);
    }
}