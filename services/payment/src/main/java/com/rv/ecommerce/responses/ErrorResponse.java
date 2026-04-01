package com.rv.ecommerce.responses;

import java.time.Instant;

public record ErrorResponse(
        String code,
        String message,
        Instant timestamp,
        String path
) {
}
