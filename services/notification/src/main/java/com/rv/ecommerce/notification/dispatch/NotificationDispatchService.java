package com.rv.ecommerce.notification.dispatch;

import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.rv.ecommerce.notification.entities.NotificationTemplateEntity;
import com.rv.ecommerce.notification.mail.AsyncMailSender;
import com.rv.ecommerce.notification.repositories.NotificationTemplateRepository;
import com.rv.notification.events.BusinessNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatchService {

    static final String TEMPLATE_ACCOUNT_CREATED = "ACCOUNT_CREATED";
    static final String TEMPLATE_PAYMENT_OUTGOING = "PAYMENT_OUTGOING";
    static final String TEMPLATE_PAYMENT_INCOMING = "PAYMENT_INCOMING";
    static final String TEMPLATE_CASHBACK_MONTHLY = "CASHBACK_MONTHLY_PAYOUT";

    private final NotificationTemplateRepository templateRepository;
    private final MustacheFactory mustacheFactory;
    private final AsyncMailSender asyncMailSender;

    @Transactional(readOnly = true)
    public void dispatch(BusinessNotificationEvent event) {
        Map<String, String> p = event.payload();
        switch (event.eventType()) {
            case ACCOUNT_CREATED -> sendOne(TEMPLATE_ACCOUNT_CREATED, p.get("ownerEmail"), p);
            case PAYMENT_COMPLETED -> {
                sendOne(TEMPLATE_PAYMENT_OUTGOING, p.get("senderEmail"), p);
                sendOne(TEMPLATE_PAYMENT_INCOMING, p.get("recipientEmail"), p);
            }
            case CASHBACK_MONTHLY_PAYOUT -> sendOne(TEMPLATE_CASHBACK_MONTHLY, p.get("recipientEmail"), p);
        }
    }

    private void sendOne(String templateKey, String emailTo, Map<String, String> payload) {
        if (emailTo == null || emailTo.isBlank()) {
            log.debug("Skip notification template={}: no recipient", templateKey);
            return;
        }
        NotificationTemplateEntity tpl = templateRepository.findByTemplateKeyAndActiveIsTrue(templateKey)
                .orElseThrow(() -> new IllegalStateException("Missing template: " + templateKey));
        Map<String, String> scope = new HashMap<>(payload);
        String subject = render(tpl.getSubjectTemplate(), scope);
        String body = render(tpl.getBodyTemplate(), scope);
        asyncMailSender.send(emailTo, subject, body);
    }

    private String render(String template, Map<String, String> scope) {
        Mustache mustache = mustacheFactory.compile(new StringReader(template), "t");
        StringWriter writer = new StringWriter();
        mustache.execute(writer, scope);
        return writer.toString();
    }
}
