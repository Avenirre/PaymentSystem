package com.rv.ecommerce.notification.repositories;

import com.rv.ecommerce.notification.entities.NotificationTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplateEntity, String> {

    Optional<NotificationTemplateEntity> findByTemplateKeyAndActiveIsTrue(String templateKey);
}
