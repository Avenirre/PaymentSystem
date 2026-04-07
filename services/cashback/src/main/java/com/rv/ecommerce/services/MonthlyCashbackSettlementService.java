package com.rv.ecommerce.services;

import com.rv.ecommerce.account.AccountApplyTransferRequest;
import com.rv.ecommerce.account.AccountTransferClient;
import com.rv.ecommerce.config.CashbackSettlementProperties;
import com.rv.ecommerce.entities.MonthlyCashbackPayoutDocument;
import com.rv.ecommerce.entities.MonthlyCashbackPayoutStatus;
import com.rv.ecommerce.repositories.MonthlyCashbackPayoutRepository;
import com.rv.ecommerce.settlement.SettlementIds;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonthlyCashbackSettlementService {

    static final String LEGAL_ENTITY_ACCRUALS = "legal_entity_cashback_accruals";
    static final String INDIVIDUAL_ACCRUALS = "individual_cashback_accruals";

    private final MongoTemplate mongoTemplate;
    private final CashbackSettlementProperties settlement;
    private final MonthlyCashbackPayoutRepository payoutRepository;
    private final AccountTransferClient accountTransferClient;

    /**
     * Settles cashback for the previous calendar month in the configured zone (payer account = {@code fromAccountNumber}).
     */
    public void settlePreviousMonth() {
        if (!settlement.enabled()) {
            log.debug("Monthly cashback settlement is disabled");
            return;
        }
        ZoneId zone = ZoneId.of(settlement.zoneId());
        YearMonth previous = YearMonth.now(zone).minusMonths(1);
        settleMonth(previous, zone);
    }

    /**
     * Aggregates accruals with {@code createdAt} in [{@code month} start, next month start) and pays each beneficiary per currency.
     */
    public void settleMonth(YearMonth yearMonth, ZoneId zone) {
        if (!settlement.enabled()) {
            return;
        }
        Instant start = yearMonth.atDay(1).atStartOfDay(zone).toInstant();
        Instant end = yearMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant();
        String ym = yearMonth.toString();

        Map<String, BigDecimal> sums = new HashMap<>();
        mergeAggregation(sums, aggregateCollection(LEGAL_ENTITY_ACCRUALS, start, end));
        mergeAggregation(sums, aggregateCollection(INDIVIDUAL_ACCRUALS, start, end));

        log.info("Monthly settlement for {}: {} payout bucket(s)", ym, sums.size());

        for (Map.Entry<String, BigDecimal> e : sums.entrySet()) {
            String[] keyParts = e.getKey().split("\\|", 2);
            if (keyParts.length != 2) {
                continue;
            }
            String beneficiary = keyParts[0];
            String currency = keyParts[1];
            BigDecimal amount = e.getValue().setScale(2, RoundingMode.HALF_UP);
            if (amount.compareTo(settlement.minimumPayoutAmount()) < 0) {
                continue;
            }
            payoutIfNeeded(ym, yearMonth, beneficiary, currency, amount);
        }
    }

    private void mergeAggregation(Map<String, BigDecimal> sums, AggregationResults<Document> results) {
        for (Document doc : results.getMappedResults()) {
            Document id = doc.get("_id", Document.class);
            if (id == null) {
                continue;
            }
            String from = id.getString("fromAccountNumber");
            String currencyCode = id.getString("currencyCode");
            if (from == null || currencyCode == null) {
                continue;
            }
            BigDecimal total = toBigDecimal(doc.get("total"));
            String key = from + "|" + currencyCode;
            sums.merge(key, total, BigDecimal::add);
        }
    }

    private AggregationResults<Document> aggregateCollection(String collection, Instant start, Instant end) {
        return mongoTemplate.aggregate(
                Aggregation.newAggregation(
                        Aggregation.match(Criteria.where("createdAt").gte(start).lt(end)),
                        Aggregation.group("fromAccountNumber", "currencyCode")
                                .sum("cashbackAmount").as("total")
                ),
                collection,
                Document.class
        );
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Decimal128 d128) {
            return d128.bigDecimalValue();
        }
        return new BigDecimal(value.toString());
    }

    private void payoutIfNeeded(String yearMonthStr, YearMonth yearMonth, String beneficiary, String currency, BigDecimal amount) {
        if (payoutRepository.existsByYearMonthAndBeneficiaryAccountNumberAndCurrencyCodeAndStatus(
                yearMonthStr, beneficiary, currency, MonthlyCashbackPayoutStatus.PAID)) {
            log.debug("Skip already paid: {} {} {}", yearMonthStr, beneficiary, currency);
            return;
        }

        String docId = SettlementIds.payoutDocumentId(yearMonth, beneficiary, currency);
        UUID transferId = SettlementIds.transferId(yearMonth, beneficiary, currency);

        AccountApplyTransferRequest request = new AccountApplyTransferRequest(
                transferId,
                settlement.systemAccountNumber(),
                beneficiary,
                amount,
                currency
        );

        try {
            accountTransferClient.applyTransfer(request);
            upsertPaid(yearMonthStr, docId, beneficiary, currency, amount, transferId);
            log.info("Monthly payout applied: period={} beneficiary={} currency={} amount={}", yearMonthStr, beneficiary, currency, amount);
        } catch (Exception ex) {
            log.error("Monthly payout failed: period={} beneficiary={} currency={} amount={}", yearMonthStr, beneficiary, currency, amount, ex);
            upsertFailed(yearMonthStr, docId, beneficiary, currency, amount, transferId, ex.getMessage());
        }
    }

    private void upsertPaid(
            String yearMonthStr,
            String docId,
            String beneficiary,
            String currency,
            BigDecimal amount,
            UUID transferId
    ) {
        MonthlyCashbackPayoutDocument doc = payoutRepository.findByYearMonthAndBeneficiaryAccountNumberAndCurrencyCode(
                        yearMonthStr, beneficiary, currency)
                .orElseGet(() -> MonthlyCashbackPayoutDocument.builder()
                        .id(docId)
                        .yearMonth(yearMonthStr)
                        .beneficiaryAccountNumber(beneficiary)
                        .currencyCode(currency)
                        .createdAt(Instant.now())
                        .build());
        doc.setAmount(amount);
        doc.setTransferId(transferId);
        doc.setStatus(MonthlyCashbackPayoutStatus.PAID);
        doc.setLastError(null);
        doc.setPaidAt(Instant.now());
        payoutRepository.save(doc);
    }

    private void upsertFailed(
            String yearMonthStr,
            String docId,
            String beneficiary,
            String currency,
            BigDecimal amount,
            UUID transferId,
            String error
    ) {
        String shortError = error != null && error.length() > 2000 ? error.substring(0, 2000) : error;
        MonthlyCashbackPayoutDocument doc = payoutRepository.findByYearMonthAndBeneficiaryAccountNumberAndCurrencyCode(
                        yearMonthStr, beneficiary, currency)
                .orElseGet(() -> MonthlyCashbackPayoutDocument.builder()
                        .id(docId)
                        .yearMonth(yearMonthStr)
                        .beneficiaryAccountNumber(beneficiary)
                        .currencyCode(currency)
                        .createdAt(Instant.now())
                        .build());
        doc.setAmount(amount);
        doc.setTransferId(transferId);
        doc.setStatus(MonthlyCashbackPayoutStatus.FAILED);
        doc.setLastError(shortError);
        doc.setPaidAt(null);
        payoutRepository.save(doc);
    }
}
