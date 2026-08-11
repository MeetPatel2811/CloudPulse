package com.cloudpulse.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

// Turns exceptions from any controller into a consistent JSON ErrorResponse with the
// right HTTP status, so the API never leaks stack traces and clients can rely on the
// shape. Extends ResponseEntityExceptionHandler so Spring MVC's own exceptions keep
// their correct status (wrong method -> 405, bad body -> 400, unsupported type -> 415,
// unknown route -> 404) while still returning our ErrorResponse body.
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // A requested service / alert / event does not exist.
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException e) {
        return build(HttpStatus.NOT_FOUND, e.getMessage());
    }

    // A bad argument reached a command or query (e.g. an unparseable value).
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        return build(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    // A delete (or similar) was blocked because the resource still has related records.
    @ExceptionHandler(ServiceHasHistoryException.class)
    public ResponseEntity<ErrorResponse> handleServiceHasHistory(ServiceHasHistoryException e) {
        return build(HttpStatus.CONFLICT, e.getMessage());
    }

    // A new maintenance window would overlap one that already exists for the service.
    @ExceptionHandler(MaintenanceWindowConflictException.class)
    public ResponseEntity<ErrorResponse> handleMaintenanceWindowConflict(MaintenanceWindowConflictException e) {
        return build(HttpStatus.CONFLICT, e.getMessage());
    }

    // Anything genuinely unexpected: log it (so it's diagnosable) and return a generic
    // 500 without leaking internals.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("Unhandled exception during an API request", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error");
    }

    // Give Spring MVC's own exceptions (405 / 415 / 400 binding, 404 route, ...) our
    // ErrorResponse body while keeping the status the framework already chose.
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        ErrorResponse error =
                new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), messageFor(ex, status));
        return new ResponseEntity<>(error, headers, statusCode);
    }

    // For a validation failure, summarize the field errors; otherwise use the status text.
    private String messageFor(Exception ex, HttpStatus status) {
        if (ex instanceof MethodArgumentNotValidException validationError) {
            String fields = validationError.getBindingResult().getFieldErrors().stream()
                    .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                    .collect(Collectors.joining("; "));
            if (!fields.isBlank()) {
                return fields;
            }
        }
        return status.getReasonPhrase();
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        ErrorResponse body =
                new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message);
        return ResponseEntity.status(status).body(body);
    }
}
