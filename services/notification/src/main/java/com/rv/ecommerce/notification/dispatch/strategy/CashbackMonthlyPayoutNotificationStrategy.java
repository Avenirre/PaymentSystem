package com.rv.ecommerce.notification.dispatch.strategy;

import com.rv.ecommerce.notification.dispatch.NotificationEventDispatchStrategy;
import com.rv.ecommerce.notification.dispatch.NotificationTemplateMailer;
import com.rv.notification.events.BusinessNotificationEvent;
import com.rv.notification.events.NotificationEventType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CashbackMonthlyPayoutNotificationStrategy implements NotificationEventDispatchStrategy {

    private static final String TEMPLATE_CASHBACK_MONTHLY = "CASHBACK_MONTHLY_PAYOUT";

    @Override
    public NotificationEventType eventType() {
        return NotificationEventType.CASHBACK_MONTHLY_PAYOUT;
    }

    @Override
    public void dispatch(BusinessNotificationEvent event, NotificationTemplateMailer mailer) {
        Map<String, String> p = event.payload();
        mailer.sendOne(TEMPLATE_CASHBACK_MONTHLY, p.get("recipientEmail"), p);
    }
}
