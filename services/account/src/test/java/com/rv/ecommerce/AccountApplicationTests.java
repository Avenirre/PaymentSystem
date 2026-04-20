package com.rv.ecommerce;

import com.rv.ecommerce.notification.BusinessNotificationPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class AccountApplicationTests {

    @MockitoBean
    private BusinessNotificationPublisher businessNotificationPublisher;

    @Test
    void contextLoads() {
    }

}
