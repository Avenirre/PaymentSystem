package com.rv.ecommerce.notification.dispatch.strategy;

import com.rv.ecommerce.notification.dispatch.NotificationEventDispatchStrategy;
import com.rv.ecommerce.notification.dispatch.NotificationTemplateMailer;
import com.rv.notification.events.BusinessNotificationEvent;
import com.rv.notification.events.NotificationEventType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AccountCreatedNotificationStrategy implements NotificationEventDispatchStrategy {

    private static final String TEMPLATE_ACCOUNT_CREATED = "ACCOUNT_CREATED";

    @Override
    public NotificationEventType eventType() {
        return NotificationEventType.ACCOUNT_CREATED;
    }

    @Override
    public void dispatch(BusinessNotificationEvent event, NotificationTemplateMailer mailer) {
        Map<String, String> p = event.payload();
        mailer.sendOne(TEMPLATE_ACCOUNT_CREATED, p.get("ownerEmail"), p);
    }
}
