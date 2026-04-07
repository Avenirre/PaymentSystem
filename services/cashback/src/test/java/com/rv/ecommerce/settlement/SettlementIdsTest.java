package com.rv.ecommerce.settlement;

import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SettlementIdsTest {

    @Test
    void transferId_isDeterministic() {
        YearMonth m = YearMonth.of(2026, 3);
        UUID a = SettlementIds.transferId(m, "acc1", "RUB");
        UUID b = SettlementIds.transferId(m, "acc1", "RUB");
        assertThat(a).isEqualTo(b);
    }

    @Test
    void transferId_differsForDifferentCurrency() {
        YearMonth m = YearMonth.of(2026, 3);
        assertThat(SettlementIds.transferId(m, "acc1", "RUB"))
                .isNotEqualTo(SettlementIds.transferId(m, "acc1", "USD"));
    }

    @Test
    void payoutDocumentId_matchesParts() {
        assertThat(SettlementIds.payoutDocumentId(YearMonth.of(2026, 3), "x", "RUB"))
                .isEqualTo("2026-03|x|RUB");
    }
}
