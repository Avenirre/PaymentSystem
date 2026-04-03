package com.rv.ecommerce.exceptions;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String accountNumber) {
        super("Insufficient funds on account: " + accountNumber);
    }
}
