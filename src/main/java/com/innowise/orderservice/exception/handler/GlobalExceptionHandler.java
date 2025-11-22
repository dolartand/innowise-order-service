package com.innowise.orderservice.exception.handler;

import com.innowise.orderservice.dto.ErrorResponseDto;
import com.innowise.orderservice.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        log.error("Resource not found: {}", ex.getMessage());

        ErrorResponseDto errorResponse = ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponse);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleUserNotFoundException(
            UserNotFoundException ex,
            HttpServletRequest request
    ) {
        log.error("User not found: {}", ex.getMessage());

        ErrorResponseDto errorResponse = ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponse);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponseDto> handleBusinessException(
            BusinessException ex,
            HttpServletRequest request
    ) {
        log.error("Business exception: {}", ex.getMessage());

        ErrorResponseDto errorResponse = ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(errorResponse);
    }

    @ExceptionHandler(InvalidOrderStateException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidOrderStateException(
            InvalidOrderStateException ex,
            HttpServletRequest request
    ) {
        log.error("Invalid order state: {}", ex.getMessage());

        ErrorResponseDto errorResponse = ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponseDto> handleServiceUnavailableException(
            ServiceUnavailableException ex,
            HttpServletRequest request
    ) {
        log.error("Service unavailable: {}", ex.getMessage());

        ErrorResponseDto errorResponse = ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public  ResponseEntity<ErrorResponseDto> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        log.error("Validation exception: {}", ex.getMessage());

        List<ErrorResponseDto.ValidationErrorDto> validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::mapToValidationError)
                .toList();

        ErrorResponseDto errorResponse = ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed")
                .path(request.getRequestURI())
                .validationErrors(validationErrors)
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unexpected error: ", ex);

        ErrorResponseDto errorResponse = ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("An unexpected error occurred. Please try again later.")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }

    private ErrorResponseDto.ValidationErrorDto mapToValidationError(FieldError fieldError) {
        return ErrorResponseDto.ValidationErrorDto.builder()
                .field(fieldError.getField())
                .rejectedValue(fieldError.getRejectedValue() != null
                ? fieldError.getRejectedValue().toString()
                : "null")
                .message(fieldError.getDefaultMessage())
                .build();
    }
}
