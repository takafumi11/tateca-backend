package com.tateca.tatecabackend.exception;

import com.tateca.tatecabackend.exception.domain.DuplicateResourceException;
import com.tateca.tatecabackend.exception.domain.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(HttpServletRequest request, IllegalArgumentException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        // 4xx = client error = WARN
        logger.warn("IllegalArgumentException at {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now().toString())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponse, status);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(HttpServletRequest request, DataAccessException ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        // 5xx = server error = ERROR
        logger.error("DataAccessException at {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now().toString())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message("Database error occurred")
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponse, status);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(HttpServletRequest request, ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        // Log based on status code (4xx = WARN, 5xx = ERROR)
        if (ex.getStatusCode().is5xxServerError()) {
            logger.error("ResponseStatusException at {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getReason(), ex);
        } else {
            logger.warn("ResponseStatusException at {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getReason());
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now().toString())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(ex.getReason())
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponse, status);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(HttpServletRequest request, ConstraintViolationException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        logger.warn("ConstraintViolationException at {} {}: {} violations",
                request.getMethod(), request.getRequestURI(),
                ex.getConstraintViolations().size());

        // Build structured field errors
        List<ErrorResponse.FieldError> fieldErrors = ex.getConstraintViolations().stream()
                .map(violation -> ErrorResponse.FieldError.builder()
                        .field(violation.getPropertyPath().toString())
                        .message(violation.getMessage())
                        .rejectedValue(violation.getInvalidValue())
                        .build())
                .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now().toString())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message("Validation failed")
                .path(request.getRequestURI())
                .errors(fieldErrors)
                .build();

        return new ResponseEntity<>(errorResponse, status);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(HttpServletRequest request, MethodArgumentNotValidException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        logger.warn("MethodArgumentNotValidException at {} {}: {} field errors",
                request.getMethod(), request.getRequestURI(),
                ex.getBindingResult().getFieldErrors().size());

        // Build structured field errors
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> ErrorResponse.FieldError.builder()
                        .field(error.getField())
                        .message(error.getDefaultMessage())
                        .rejectedValue(error.getRejectedValue())
                        .build())
                .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now().toString())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message("Validation failed")
                .path(request.getRequestURI())
                .errors(fieldErrors)
                .build();

        return new ResponseEntity<>(errorResponse, status);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(HttpServletRequest request, MethodArgumentTypeMismatchException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String paramName = ex.getName();
        String paramType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String message = String.format("Invalid format for parameter '%s': expected %s", paramName, paramType);
        logger.warn("MethodArgumentTypeMismatchException at {} {}: {}", request.getMethod(), request.getRequestURI(), message);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now().toString())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponse, status);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpServletRequest request, HttpMessageNotReadableException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = "Invalid request body format. Please check the data types and structure.";

        // Extract detailed error information from Jackson's InvalidFormatException
        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException invalidFormatException) {
            String fieldName = invalidFormatException.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .collect(Collectors.joining("."));
            String targetType = invalidFormatException.getTargetType().getSimpleName();
            Object value = invalidFormatException.getValue();
            String valueType = value != null ? value.getClass().getSimpleName() : "null";

            message = String.format("Invalid value for field '%s': expected %s but received %s",
                    fieldName, targetType, valueType);
        }

        logger.warn("HttpMessageNotReadableException at {} {}: {}", request.getMethod(), request.getRequestURI(), message);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now().toString())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponse, status);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(HttpServletRequest request, HttpMediaTypeNotSupportedException ex) {
        HttpStatus status = HttpStatus.UNSUPPORTED_MEDIA_TYPE;
        String message = "Unsupported Media Type. Content-Type must be application/json";
        logger.warn("HttpMediaTypeNotSupportedException at {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now().toString())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponse, status);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(HttpServletRequest request, EntityNotFoundException ex) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        logger.warn("EntityNotFoundException at {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now().toString())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponse, status);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(HttpServletRequest request, DuplicateResourceException ex) {
        HttpStatus status = HttpStatus.CONFLICT;
        logger.warn("DuplicateResourceException at {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now().toString())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponse, status);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(HttpServletRequest request, DataIntegrityViolationException ex) {
        HttpStatus status = HttpStatus.CONFLICT;
        logger.warn("DataIntegrityViolationException at {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now().toString())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message("Database constraint violation")
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponse, status);
    }

    /**
     * Catch-all handler for unexpected exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(HttpServletRequest request, Exception ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        logger.error("Unexpected exception at {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now().toString())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message("An unexpected error occurred")
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponse, status);
    }
}
