package com.rv.ecommerce.repositories;

import com.rv.ecommerce.entities.MonthlyCashbackPayoutDocument;
import com.rv.ecommerce.entities.MonthlyCashbackPayoutStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface MonthlyCashbackPayoutRepository extends MongoRepository<MonthlyCashbackPayoutDocument, String> {

    Optional<MonthlyCashbackPayoutDocument> findByYearMonthAndBeneficiaryAccountNumberAndCurrencyCode(
            String yearMonth,
            String beneficiaryAccountNumber,
            String currencyCode
    );

    boolean existsByYearMonthAndBeneficiaryAccountNumberAndCurrencyCodeAndStatus(
            String yearMonth,
            String beneficiaryAccountNumber,
            String currencyCode,
            MonthlyCashbackPayoutStatus status
    );
}
