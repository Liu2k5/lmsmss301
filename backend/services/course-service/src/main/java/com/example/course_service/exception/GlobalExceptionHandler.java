package com.example.course_service.exception;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
		return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<ApiError> handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of());
	}

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ApiError> handleConflict(ConflictException ex, HttpServletRequest request) {
		return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
		log.warn("Vi phạm ràng buộc dữ liệu: {}", ex.getMostSpecificCause().getMessage());
		return build(HttpStatus.CONFLICT, "Dữ liệu vi phạm ràng buộc, vui lòng kiểm tra lại", request, List.of());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		List<FieldErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> new FieldErrorDetail(error.getField(), error.getDefaultMessage()))
				.toList();
		return build(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ", request, details);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, "Nội dung JSON không hợp lệ hoặc sai kiểu dữ liệu", request, List.of());
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
		return build(HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên", request, List.of());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("Lỗi không mong đợi tại {}", request.getRequestURI(), ex);
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "Đã xảy ra lỗi hệ thống", request, List.of());
	}

	private ResponseEntity<ApiError> build(HttpStatus status, String message,
			HttpServletRequest request, List<FieldErrorDetail> details) {
		ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(),
				message, request.getRequestURI(), details);
		return ResponseEntity.status(status).body(body);
	}
}
