package com.rv.ecommerce.repositories;

import com.rv.ecommerce.entities.CashbackOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CashbackOutboxRepository extends JpaRepository<CashbackOutbox, UUID> {

    @Query(value = """
            SELECT * FROM cashback_outbox
            WHERE status = 'PENDING'
            ORDER BY created_at
            FOR UPDATE SKIP LOCKED
            LIMIT :limit
            """, nativeQuery = true)
    List<CashbackOutbox> lockPendingBatch(@Param("limit") int limit);
}
