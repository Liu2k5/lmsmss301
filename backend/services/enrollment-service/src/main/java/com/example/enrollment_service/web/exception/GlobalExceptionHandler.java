package com.example.enrollment_service.web.exception;

import com.example.enrollment_service.web.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex) {
		Map<String, String> fields = ex.getBindingResult().getFieldErrors().stream()
				.collect(Collectors.toMap(f -> f.getField(), f -> String.valueOf(f.getDefaultMessage()), (a, b) -> a));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ErrorResponse("VALIDATION_ERROR", "Invalid request", fields));
	}

	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<ErrorResponse> notFound(NotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ErrorResponse("NOT_FOUND", ex.getMessage(), null));
	}

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ErrorResponse> conflict(ConflictException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(new ErrorResponse("CONFLICT", ex.getMessage(), null));
	}
}
