package com.rv.ecommerce.repositories;

import com.rv.ecommerce.entities.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<AccountEntity, String> {
}
