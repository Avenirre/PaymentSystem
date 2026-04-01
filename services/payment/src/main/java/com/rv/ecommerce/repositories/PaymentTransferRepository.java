package com.rv.ecommerce.repositories;

import com.rv.ecommerce.entities.PaymentTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentTransferRepository extends JpaRepository<PaymentTransfer, UUID> {
}
