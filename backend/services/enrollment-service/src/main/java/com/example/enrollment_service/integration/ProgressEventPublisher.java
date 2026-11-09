package com.example.enrollment_service.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Báo cho Progress Service biết học viên vừa được cấp quyền học, để tạo bản ghi tiến độ ban đầu.
 * Queue do Progress Service khai báo; ở đây publish qua default exchange với routing key = tên queue.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProgressEventPublisher {

	public static final String STUDENT_ENROLLED_QUEUE = "student.enrolled.queue";

	private final RabbitTemplate rabbitTemplate;
	private final CourseCatalogClient courseCatalog;
	private final ObjectMapper objectMapper;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onEnrollmentActivated(EnrollmentActivatedEvent event) {
		List<UUID> lessonIds = courseCatalog.publishedLessonIds(event.courseId());
		if (lessonIds.isEmpty()) {
			// Khoá học chưa xuất bản thì chưa có nội dung để theo dõi tiến độ.
			log.info("Bỏ qua tạo tiến độ cho khoá {}: chưa có bài học đã xuất bản", event.courseId());
			return;
		}

		Map<String, Object> payload = new LinkedHashMap<>();
		// Dùng enrollmentId làm eventId để Progress Service chống xử lý trùng khi enroll lại.
		payload.put("eventId", event.enrollmentId().toString());
		payload.put("studentId", event.studentId().toString());
		payload.put("courseId", event.courseId().toString());
		payload.put("lessonIds", lessonIds.stream().map(UUID::toString).toList());

		try {
			rabbitTemplate.convertAndSend(STUDENT_ENROLLED_QUEUE, objectMapper.writeValueAsString(payload),
					message -> {
						message.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
						return message;
					});
			log.info("Đã gửi student.enrolled cho học viên {} / khoá {}", event.studentId(), event.courseId());
		} catch (RuntimeException e) {
			// Không để lỗi broker làm hỏng việc đăng ký khoá học đã commit.
			log.warn("Không gửi được student.enrolled cho khoá {}: {}", event.courseId(), e.getMessage());
		}
	}
}
