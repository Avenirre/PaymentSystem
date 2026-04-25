package com.rv.ecommerce.notification.dispatch;

import com.rv.notification.events.BusinessNotificationEvent;
import com.rv.notification.events.NotificationEventType;

public interface NotificationEventDispatchStrategy {

    NotificationEventType eventType();

    void dispatch(BusinessNotificationEvent event, NotificationTemplateMailer mailer);
}
