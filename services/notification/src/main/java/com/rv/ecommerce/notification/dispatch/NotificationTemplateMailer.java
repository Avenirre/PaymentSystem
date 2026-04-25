package com.rv.ecommerce.notification.dispatch;

import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.rv.ecommerce.notification.entities.NotificationTemplateEntity;
import com.rv.ecommerce.notification.mail.AsyncMailSender;
import com.rv.ecommerce.notification.repositories.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationTemplateMailer {

    private final NotificationTemplateRepository templateRepository;
    private final MustacheFactory mustacheFactory;
    private final AsyncMailSender asyncMailSender;

    public void sendOne(String templateKey, String emailTo, Map<String, String> payload) {
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
