package com.firstclub.membership.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MembershipException.class)
	public ResponseEntity<Map<String, Object>> handleMembership(MembershipException ex) {
		HttpStatus status = "CONCURRENT_MODIFICATION".equals(ex.getErrorCode())
				? HttpStatus.CONFLICT
				: HttpStatus.BAD_REQUEST;
		return buildResponse(status, ex);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
		String details = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.joining(", "));
		return ResponseEntity.badRequest().body(Map.of(
				"timestamp", Instant.now().toString(),
				"errorCode", "VALIDATION_ERROR",
				"message", details));
	}

	private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, MembershipException ex) {
		return ResponseEntity.status(status).body(Map.of(
				"timestamp", Instant.now().toString(),
				"errorCode", ex.getErrorCode(),
				"message", ex.getMessage()));
	}
}
