package com.example.progress_service.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Trả lỗi dạng JSON có `message` để UI hiển thị được lý do
 * (mặc định Spring bỏ message, client chỉ thấy "Forbidden").
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException ex, HttpServletRequest request) {
		HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
		String message = ex.getReason() == null ? "Yêu cầu không hợp lệ" : ex.getReason();
		return body(status == null ? HttpStatus.BAD_REQUEST : status, message, request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(error -> error.getDefaultMessage())
				.orElse("Dữ liệu không hợp lệ");
		return body(HttpStatus.BAD_REQUEST, message, request);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex,
			HttpServletRequest request) {
		return body(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
	}

	private static ResponseEntity<Map<String, Object>> body(HttpStatus status, String message,
			HttpServletRequest request) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("timestamp", Instant.now());
		payload.put("status", status.value());
		payload.put("error", status.getReasonPhrase());
		payload.put("message", message);
		payload.put("path", request.getRequestURI());
		return ResponseEntity.status(status).body(payload);
	}
}
