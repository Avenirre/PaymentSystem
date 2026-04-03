package com.rv.ecommerce.exceptions;

import com.rv.ecommerce.responses.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CashbackServiceException.class)
    public ResponseEntity<ErrorResponse> handleCashback(
            CashbackServiceException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse(
                        "CASHBACK_SERVICE_ERROR",
                        exception.getMessage(),
                        Instant.now(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(AccountOperationException.class)
    public ResponseEntity<ErrorResponse> handleAccountOperation(
            AccountOperationException exception,
            HttpServletRequest request
    ) {
        HttpStatus httpStatus = switch (exception.getStatus()) {
            case 404 -> HttpStatus.BAD_REQUEST;
            case 409 -> HttpStatus.CONFLICT;
            default -> exception.getStatus() >= 500 ? HttpStatus.BAD_GATEWAY : HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(httpStatus)
                .body(new ErrorResponse(
                        "ACCOUNT_SERVICE_ERROR",
                        exception.getMessage(),
                        Instant.now(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Validation failed");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        "VALIDATION_ERROR",
                        message,
                        Instant.now(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnhandled(
            Exception exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        "INTERNAL_ERROR",
                        "Unexpected server error",
                        Instant.now(),
                        request.getRequestURI()
                ));
    }
}
