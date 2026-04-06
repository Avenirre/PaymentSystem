package com.rv.ecommerce.repositories;

import com.rv.ecommerce.entities.IndividualCashbackAccrualDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface IndividualCashbackAccrualRepository extends MongoRepository<IndividualCashbackAccrualDocument, UUID> {
}
