package com.resumebackend.payments.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    public record ErrorBody(String code, String message, Instant timestamp) {}

    private static ResponseEntity<Object> body(HttpStatusCode status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorBody(code, message, Instant.now()));
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Object> api(ApiException e) { return body(e.getStatus(), e.getCode(), e.getMessage()); }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<Object> conflict(ObjectOptimisticLockingFailureException e) {
        return body(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION", "Payment is being processed by another worker, retry shortly");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> other(Exception e) {
        log.error("Unhandled error", e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected error");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " " + f.getDefaultMessage()).collect(Collectors.joining("; "));
        return body(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", msg);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object b, HttpHeaders headers,
                                                             HttpStatusCode status, WebRequest request) {
        return body(status, status instanceof HttpStatus hs ? hs.name() : "ERROR", ex.getMessage());
    }
}
