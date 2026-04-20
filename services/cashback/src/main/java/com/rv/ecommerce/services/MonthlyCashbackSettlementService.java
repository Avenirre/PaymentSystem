package com.rv.ecommerce.services;

import com.rv.ecommerce.account.AccountApplyTransferRequest;
import com.rv.ecommerce.account.AccountTransferClient;
import com.rv.ecommerce.config.CashbackSettlementProperties;
import com.rv.ecommerce.notification.CashbackNotificationPublisher;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final CashbackNotificationPublisher cashbackNotificationPublisher;

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

        Map<String, BigDecimal> sums = aggregateTotals(start, end);

        log.info("Monthly settlement for {}: {} payout bucket(s)", ym, sums.size());

        sums.entrySet().stream()
                .map(e -> PayoutLine.fromEntry(e.getKey(), e.getValue()))
                .flatMap(Optional::stream)
                .filter(line -> line.amount().compareTo(settlement.minimumPayoutAmount()) >= 0)
                .forEach(line -> payoutIfNeeded(ym, yearMonth, line.beneficiary(), line.currency(), line.amount()));
    }

    private Map<String, BigDecimal> aggregateTotals(Instant start, Instant end) {
        return Stream.of(LEGAL_ENTITY_ACCRUALS, INDIVIDUAL_ACCRUALS)
                .flatMap(collection -> aggregateCollection(collection, start, end).getMappedResults().stream())
                .map(this::toAmountBucket)
                .flatMap(Optional::stream)
                .collect(Collectors.toMap(AmountBucket::key, AmountBucket::amount, BigDecimal::add));
    }

    private Optional<AmountBucket> toAmountBucket(Document doc) {
        Document id = doc.get("_id", Document.class);
        if (id == null) {
            return Optional.empty();
        }
        String from = id.getString("fromAccountNumber");
        String currencyCode = id.getString("currencyCode");
        if (from == null || currencyCode == null) {
            return Optional.empty();
        }
        String key = from + "|" + currencyCode;
        return Optional.of(new AmountBucket(key, toBigDecimal(doc.get("total"))));
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
        return switch (value) {
            case null -> BigDecimal.ZERO;
            case BigDecimal bd -> bd;
            case Decimal128 d128 -> d128.bigDecimalValue();
            default -> new BigDecimal(value.toString());
        };
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
            if (settlement.payoutNotificationEmail() != null && !settlement.payoutNotificationEmail().isBlank()) {
                cashbackNotificationPublisher.publishMonthlyCashbackPayout(
                        yearMonthStr, beneficiary, currency, amount, transferId, settlement.payoutNotificationEmail());
            }
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

    private record AmountBucket(String key, BigDecimal amount) {
    }

    private record PayoutLine(String beneficiary, String currency, BigDecimal amount) {
        static Optional<PayoutLine> fromEntry(String compositeKey, BigDecimal rawTotal) {
            String[] parts = compositeKey.split("\\|", 2);
            if (parts.length != 2) {
                return Optional.empty();
            }
            BigDecimal amount = rawTotal.setScale(2, RoundingMode.HALF_UP);
            return Optional.of(new PayoutLine(parts[0], parts[1], amount));
        }
    }
}
