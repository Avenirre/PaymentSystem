package com.rv.ecommerce.notification.mail;

import com.rv.ecommerce.notification.config.NotificationProperties;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncMailSender {

    private final JavaMailSender mailSender;
    private final NotificationProperties notificationProperties;

    @Async("notificationTaskExecutor")
    public void send(String to, String subject, String textBody) {
        if (to == null || to.isBlank()) {
            log.debug("Skip mail: empty recipient");
            return;
        }
        String recipient = notificationProperties.forceToAddress() != null
                && !notificationProperties.forceToAddress().isBlank()
                ? notificationProperties.forceToAddress()
                : to;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(textBody, false);
            mailSender.send(message);
            log.info("Mail queued/sent to {}", recipient);
        } catch (Exception e) {
            log.error("Failed to send mail to {}", recipient, e);
        }
    }
}
