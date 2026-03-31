package com.rv.ecommerce.exceptions;

public class AccountCreationException extends RuntimeException {

    public AccountCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
