package com.rv.ecommerce.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class AccountOperationException extends RuntimeException {

    private final int status;

    public AccountOperationException(int status, String message) {
        super(message);
        this.status = status;
    }

    public AccountOperationException(HttpStatusCode statusCode, String message) {
        this(statusCode.value(), message);
    }

    public AccountOperationException(String message, Throwable cause) {
        super(message, cause);
        this.status = 502;
    }
}
