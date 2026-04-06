package com.rv.ecommerce.repositories;

import com.rv.ecommerce.entities.LegalEntityCashbackAccrualDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface LegalEntityCashbackAccrualRepository extends MongoRepository<LegalEntityCashbackAccrualDocument, UUID> {
}
