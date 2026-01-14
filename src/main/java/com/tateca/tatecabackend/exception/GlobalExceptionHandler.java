package com.tateca.tatecabackend.exception;

import com.tateca.tatecabackend.exception.domain.BusinessRuleViolationException;
import com.tateca.tatecabackend.exception.domain.DuplicateResourceException;
import com.tateca.tatecabackend.exception.domain.EntityNotFoundException;
import com.tateca.tatecabackend.util.MessageResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
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
import java.util.stream.Collectors;

/**
 * Global exception handler with internationalization support.
 *
 * Architecture:
 * - Uses MessageResolver for localized error messages
 * - Detects locale from Accept-Language header
 * - Returns structured ErrorResponse with error codes
 * - Maintains backward compatibility during migration
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final MessageResolver messageResolver;

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(HttpServletRequest request, IllegalArgumentException ex) {
        logger.warn("IllegalArgumentException at {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode(ErrorCode.VALIDATION_FAILED.getCode())
                .message(ex.getMessage()) // Keep original message for backward compatibility
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(HttpServletRequest request, DataAccessException ex) {
        logger.error("DataAccessException at {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);

        String message = messageResolver.getMessage(ErrorCode.DATABASE_ERROR);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .errorCode(ErrorCode.DATABASE_ERROR.getCode())
                .message(message)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(HttpServletRequest request, ResponseStatusException ex) {
        if (ex.getStatusCode().is5xxServerError()) {
            logger.error("ResponseStatusException at {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getReason(), ex);
        } else {
            logger.warn("ResponseStatusException at {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getReason());
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(ex.getStatusCode().value())
                .message(ex.getReason())
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponse, ex.getStatusCode());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(HttpServletRequest request, ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        logger.warn("ConstraintViolationException at {} {}: {}", request.getMethod(), request.getRequestURI(), message);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode(ErrorCode.VALIDATION_FAILED.getCode())
                .message(message)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(HttpServletRequest request, MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        logger.warn("MethodArgumentNotValidException at {} {}: {}", request.getMethod(), request.getRequestURI(), message);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode(ErrorCode.VALIDATION_FAILED.getCode())
                .message(message)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(HttpServletRequest request, MethodArgumentTypeMismatchException ex) {
        String paramName = ex.getName();
        String paramType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String valueType = ex.getValue() != null ? ex.getValue().getClass().getSimpleName() : "null";

        String message = messageResolver.getMessage(
                ErrorCode.REQUEST_INVALID_PARAMETER,
                paramName, paramType, valueType
        );

        logger.warn("MethodArgumentTypeMismatchException at {} {}: {}", request.getMethod(), request.getRequestURI(), message);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode(ErrorCode.REQUEST_INVALID_PARAMETER.getCode())
                .message(message)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpServletRequest request, HttpMessageNotReadableException ex) {
        String message = messageResolver.getMessage(ErrorCode.REQUEST_INVALID_FORMAT);

        // Extract detailed error from Jackson
        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException invalidFormatException) {
            String fieldName = invalidFormatException.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .collect(Collectors.joining("."));
            String targetType = invalidFormatException.getTargetType().getSimpleName();
            Object value = invalidFormatException.getValue();
            String valueType = value != null ? value.getClass().getSimpleName() : "null";

            message = messageResolver.getMessage(
                    ErrorCode.REQUEST_INVALID_PARAMETER,
                    fieldName, targetType, valueType
            );
        }

        logger.warn("HttpMessageNotReadableException at {} {}: {}", request.getMethod(), request.getRequestURI(), message);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode(ErrorCode.REQUEST_INVALID_FORMAT.getCode())
                .message(message)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(HttpServletRequest request, HttpMediaTypeNotSupportedException ex) {
        String message = messageResolver.getMessage(ErrorCode.REQUEST_UNSUPPORTED_MEDIA_TYPE);
        logger.warn("HttpMediaTypeNotSupportedException at {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value())
                .errorCode(ErrorCode.REQUEST_UNSUPPORTED_MEDIA_TYPE.getCode())
                .message(message)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(HttpServletRequest request, EntityNotFoundException ex) {
        logger.warn("EntityNotFoundException at {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());

        String message = messageResolver.getMessage(ex.getErrorCode(), ex.getMessageArgs());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .errorCode(ex.getErrorCode().getCode())
                .message(message)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(HttpServletRequest request, DuplicateResourceException ex) {
        logger.warn("DuplicateResourceException at {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());

        String message = messageResolver.getMessage(ex.getErrorCode(), ex.getMessageArgs());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.CONFLICT.value())
                .errorCode(ex.getErrorCode().getCode())
                .message(message)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(HttpServletRequest request, DataIntegrityViolationException ex) {
        logger.warn("DataIntegrityViolationException at {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());

        String message = messageResolver.getMessage(ErrorCode.DATABASE_CONSTRAINT_VIOLATION);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.CONFLICT.value())
                .errorCode(ErrorCode.DATABASE_CONSTRAINT_VIOLATION.getCode())
                .message(message)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRuleViolationException(HttpServletRequest request, BusinessRuleViolationException ex) {
        logger.warn("BusinessRuleViolationException at {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());

        String message = messageResolver.getMessage(ex.getErrorCode(), ex.getMessageArgs());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode(ex.getErrorCode().getCode())
                .message(message)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Catch-all handler for unexpected exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(HttpServletRequest request, Exception ex) {
        logger.error("Unexpected exception at {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);

        String message = messageResolver.getMessage(ErrorCode.SYSTEM_UNEXPECTED_ERROR);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .errorCode(ErrorCode.SYSTEM_UNEXPECTED_ERROR.getCode())
                .message(message)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
