package com.rv.ecommerce.notification.dispatch;

import com.rv.notification.events.BusinessNotificationEvent;
import com.rv.notification.events.NotificationEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationDispatchService {

    private final Map<NotificationEventType, NotificationEventDispatchStrategy> strategies;
    private final NotificationTemplateMailer mailer;

    @Transactional(readOnly = true)
    public void dispatch(BusinessNotificationEvent event) {
        NotificationEventDispatchStrategy strategy = strategies.get(event.eventType());
        if (strategy == null) {
            throw new IllegalStateException("No dispatch strategy for: " + event.eventType());
        }
        strategy.dispatch(event, mailer);
    }
}
