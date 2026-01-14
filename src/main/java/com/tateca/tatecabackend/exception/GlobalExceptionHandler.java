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
import java.util.List;
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

        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now().toString())
                .status(httpStatus.value())
                .error(httpStatus.getReasonPhrase())
                .errorCode(ErrorCode.VALIDATION_FAILED.getCode())
                .message(ex.getMessage()) // Keep original message for backward compatibility
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponse, httpStatus);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(HttpServletRequest request, DataAccessException ex) {
        logger.error("DataAccessException at {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);

        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = messageResolver.getMessage(ErrorCode.DATABASE_ERROR);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now().toString())
                .status(httpStatus.value())
                .error(httpStatus.getReasonPhrase())
                .errorCode(ErrorCode.DATABASE_ERROR.getCode())
                .message(message)
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponse, httpStatus);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(HttpServletRequest request, ResponseStatusException ex) {
        if (ex.getStatusCode().is5xxServerError()) {
            logger.error("ResponseStatusException at {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getReason(), ex);
        } else {
            logger.warn("ResponseStatusException at {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getReason());
        }

        HttpStatus httpStatus = HttpStatus.resolve(ex.getStatusCode().value());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now().toString())
                .status(ex.getStatusCode().value())
                .error(httpStatus != null ? httpStatus.getReasonPhrase() : "Unknown")
                .message(ex.getReason())
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponse, ex.getStatusCode());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(HttpServletRequest request, ConstraintViolationException ex) {
        logger.warn("ConstraintViolationException at {} {}: {} violations",
                request.getMethod(), request.getRequestURI(), ex.getConstraintViolations().size());

        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
        String message = messageResolver.getMessage(ErrorCode.VALIDATION_FAILED);

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
                .status(httpStatus.value())
                .error(httpStatus.getReasonPhrase())
                .errorCode(ErrorCode.VALIDATION_FAILED.getCode())
                .message(message)
                .path(request.getRequestURI())
                .errors(fieldErrors)
                .build();

        return new ResponseEntity<>(errorResponse, httpStatus);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(HttpServletRequest request, MethodArgumentNotValidException ex) {
        logger.warn("MethodArgumentNotValidException at {} {}: {} field errors",
                request.getMethod(), request.getRequestURI(), ex.getBindingResult().getFieldErrors().size());

        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
        String message = messageResolver.getMessage(ErrorCode.VALIDATION_FAILED);

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
                .status(httpStatus.value())
                .error(httpStatus.getReasonPhrase())
                .errorCode(ErrorCode.VALIDATION_FAILED.getCode())
                .message(message)
                .path(request.getRequestURI())
                .errors(fieldErrors)
                .build();

        return new ResponseEntity<>(errorResponse, httpStatus);
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

        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now().toString())
                .status(httpStatus.value())
                .error(httpStatus.getReasonPhrase())
                .errorCode(ErrorCode.REQUEST_INVALID_PARAMETER.getCode())
                .message(message)
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponse, httpStatus);
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

        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now().toString())
                .status(httpStatus.value())
                .error(httpStatus.getReasonPhrase())
                .errorCode(ErrorCode.REQUEST_INVALID_FORMAT.getCode())
                .message(message)
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponse, httpStatus);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(HttpServletRequest request, HttpMediaTypeNotSupportedException ex) {
        String message = messageResolver.getMessage(ErrorCode.REQUEST_UNSUPPORTED_MEDIA_TYPE);
        logger.warn("HttpMediaTypeNotSupportedException at {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());

        HttpStatus httpStatus = HttpStatus.UNSUPPORTED_MEDIA_TYPE;
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now().toString())
                .status(httpStatus.value())
                .error(httpStatus.getReasonPhrase())
                .errorCode(ErrorCode.REQUEST_UNSUPPORTED_MEDIA_TYPE.getCode())
                .message(message)
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponse, httpStatus);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(HttpServletRequest request, EntityNotFoundException ex) {
        // Log with technical details for debugging
        if (ex.getMessageArgs() != null && ex.getMessageArgs().length > 0) {
            logger.warn("EntityNotFoundException at {} {}: {} (args: {})",
                request.getMethod(), request.getRequestURI(), ex.getErrorCode().getCode(),
                java.util.Arrays.toString(ex.getMessageArgs()));
        } else {
            logger.warn("EntityNotFoundException at {} {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getErrorCode().getCode());
        }

        HttpStatus httpStatus = ex.getErrorCode().getHttpStatus();
        String message = messageResolver.getMessage(ex.getErrorCode(), ex.getMessageArgs());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now().toString())
                .status(httpStatus.value())
                .error(httpStatus.getReasonPhrase())
                .errorCode(ex.getErrorCode().getCode())
                .message(message)
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(errorResponse, httpStatus);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(HttpServletRequest request, DuplicateResourceException ex) {
        // Log with technical details for debugging
        if (ex.getMessageArgs() != null && ex.getMessageArgs().length > 0) {
            logger.warn("DuplicateResourceException at {} {}: {} (args: {})",
                request.getMethod(), request.getRequestURI(), ex.getErrorCode().getCode(),
                java.util.Arrays.toString(ex.getMessageArgs()));
        } else {
            logger.warn("DuplicateResourceException at {} {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getErrorCode().getCode());
        }

        HttpStatus httpStatus = ex.getErrorCode().getHttpStatus();
        String message = messageResolver.getMessage(ex.getErrorCode(), ex.getMessageArgs());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now().toString())
                .status(httpStatus.value())
                .error(httpStatus.getReasonPhrase())
                .errorCode(ex.getErrorCode().getCode())
                .message(message)
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(errorResponse, httpStatus);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(HttpServletRequest request, DataIntegrityViolationException ex) {
        logger.warn("DataIntegrityViolationException at {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());

        HttpStatus httpStatus = HttpStatus.CONFLICT;
        String message = messageResolver.getMessage(ErrorCode.DATABASE_CONSTRAINT_VIOLATION);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now().toString())
                .status(httpStatus.value())
                .error(httpStatus.getReasonPhrase())
                .errorCode(ErrorCode.DATABASE_CONSTRAINT_VIOLATION.getCode())
                .message(message)
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponse, httpStatus);
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRuleViolationException(HttpServletRequest request, BusinessRuleViolationException ex) {
        // Log with technical details for debugging
        if (ex.getMessageArgs() != null && ex.getMessageArgs().length > 0) {
            logger.warn("BusinessRuleViolationException at {} {}: {} (args: {})",
                request.getMethod(), request.getRequestURI(), ex.getErrorCode().getCode(),
                java.util.Arrays.toString(ex.getMessageArgs()));
        } else {
            logger.warn("BusinessRuleViolationException at {} {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getErrorCode().getCode());
        }

        HttpStatus httpStatus = ex.getErrorCode().getHttpStatus();
        String message = messageResolver.getMessage(ex.getErrorCode(), ex.getMessageArgs());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now().toString())
                .status(httpStatus.value())
                .error(httpStatus.getReasonPhrase())
                .errorCode(ex.getErrorCode().getCode())
                .message(message)
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(errorResponse, httpStatus);
    }

    /**
     * Catch-all handler for unexpected exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(HttpServletRequest request, Exception ex) {
        logger.error("Unexpected exception at {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);

        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = messageResolver.getMessage(ErrorCode.SYSTEM_UNEXPECTED_ERROR);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now().toString())
                .status(httpStatus.value())
                .error(httpStatus.getReasonPhrase())
                .errorCode(ErrorCode.SYSTEM_UNEXPECTED_ERROR.getCode())
                .message(message)
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponse, httpStatus);
    }
}
