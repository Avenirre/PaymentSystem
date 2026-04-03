package com.rv.ecommerce.requests;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record CompensateTransferRequest(@NotNull UUID transferId) {
}
