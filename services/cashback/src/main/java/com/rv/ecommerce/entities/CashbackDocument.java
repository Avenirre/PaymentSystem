package com.rv.ecommerce.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "cashback_rates")
public class CashbackDocument {

    /**
     * {@link CashbackType#name()}
     */
    @Id
    private String id;

    /**
     * Cashback percent (0–100, e.g. 1.5 means 1.5%).
     */
    private BigDecimal percent;
}
