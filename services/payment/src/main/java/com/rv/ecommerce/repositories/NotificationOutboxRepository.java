package com.rv.ecommerce.repositories;

import com.rv.ecommerce.entities.NotificationOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, UUID> {

    @Query(value = """
            SELECT * FROM notification_outbox
            WHERE status = 'PENDING'
            ORDER BY created_at
            FOR UPDATE SKIP LOCKED
            LIMIT :limit
            """, nativeQuery = true)
    List<NotificationOutbox> lockPendingBatch(@Param("limit") int limit);
}
