package com.rv.ecommerce.exceptions;

import java.util.UUID;

public class AccountTransferConflictException extends RuntimeException {

    public AccountTransferConflictException(UUID transferId, String message) {
        super("Transfer " + transferId + ": " + message);
    }
}
