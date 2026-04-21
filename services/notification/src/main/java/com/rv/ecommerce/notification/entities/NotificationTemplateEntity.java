package com.rv.ecommerce.notification.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "notification_templates")
public class NotificationTemplateEntity {

    @Id
    @Column(name = "template_key", length = 64, nullable = false)
    private String templateKey;

    @Column(name = "subject_template", nullable = false, columnDefinition = "text")
    private String subjectTemplate;

    @Column(name = "body_template", nullable = false, columnDefinition = "text")
    private String bodyTemplate;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
