package com.rv.ecommerce.notification.dispatch.strategy;

import com.rv.ecommerce.notification.dispatch.NotificationEventDispatchStrategy;
import com.rv.ecommerce.notification.dispatch.NotificationTemplateMailer;
import com.rv.notification.events.BusinessNotificationEvent;
import com.rv.notification.events.NotificationEventType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PaymentCompletedNotificationStrategy implements NotificationEventDispatchStrategy {

    private static final String TEMPLATE_PAYMENT_OUTGOING = "PAYMENT_OUTGOING";
    private static final String TEMPLATE_PAYMENT_INCOMING = "PAYMENT_INCOMING";

    @Override
    public NotificationEventType eventType() {
        return NotificationEventType.PAYMENT_COMPLETED;
    }

    @Override
    public void dispatch(BusinessNotificationEvent event, NotificationTemplateMailer mailer) {
        Map<String, String> p = event.payload();
        mailer.sendOne(TEMPLATE_PAYMENT_OUTGOING, p.get("senderEmail"), p);
        mailer.sendOne(TEMPLATE_PAYMENT_INCOMING, p.get("recipientEmail"), p);
    }
}
