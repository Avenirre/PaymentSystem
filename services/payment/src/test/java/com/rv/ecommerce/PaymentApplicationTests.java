package com.rv.ecommerce;

import com.rv.ecommerce.outbox.CashbackOutboxPublisher;
import com.rv.ecommerce.outbox.NotificationOutboxPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class PaymentApplicationTests {

    @MockitoBean
    private CashbackOutboxPublisher cashbackOutboxPublisher;

    @MockitoBean
    private NotificationOutboxPublisher notificationOutboxPublisher;

    @Test
    void contextLoads() {
    }
}
