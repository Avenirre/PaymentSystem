package com.rv.ecommerce.repositories;

import com.rv.ecommerce.entities.AccountTransferLedger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountTransferLedgerRepository extends JpaRepository<AccountTransferLedger, UUID> {
}
