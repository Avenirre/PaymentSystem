package com.rv.ecommerce.settlement;

import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.UUID;

public final class SettlementIds {

    private SettlementIds() {
    }

    public static String payoutDocumentId(YearMonth yearMonth, String beneficiaryAccountNumber, String currencyCode) {
        return yearMonth + "|" + beneficiaryAccountNumber + "|" + currencyCode;
    }

    public static UUID transferId(YearMonth yearMonth, String beneficiaryAccountNumber, String currencyCode) {
        String payload = "cashback-monthly|" + yearMonth + "|" + beneficiaryAccountNumber + "|" + currencyCode;
        return UUID.nameUUIDFromBytes(payload.getBytes(StandardCharsets.UTF_8));
    }
}
