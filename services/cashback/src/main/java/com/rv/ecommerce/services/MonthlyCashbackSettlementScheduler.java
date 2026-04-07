package com.rv.ecommerce.services;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "cashback.settlement", name = "enabled", havingValue = "true")
public class MonthlyCashbackSettlementScheduler {

    private final MonthlyCashbackSettlementService settlementService;

    /**
     * First day of each month at 00:05 in the configured zone — pays the previous calendar month.
     */
    @Scheduled(cron = "${cashback.settlement.schedule-cron}", zone = "${cashback.settlement.zone-id}")
    public void runMonthlySettlement() {
        settlementService.settlePreviousMonth();
    }
}
