package com.rv.ecommerce.repositories;

import com.rv.ecommerce.entities.CashbackDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CashbackRepository extends MongoRepository<CashbackDocument, String> {
}
