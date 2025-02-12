package com.tateca.tatecabackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(ExchangeRateId.class)
@Table(name = "exchange_rates")
public class ExchangeRateEntity {
    @Id
    @Column(name = "currency_code", nullable = false)
    private String currencyCode;

    @ManyToOne
    @JoinColumn(name = "currency_code", referencedColumnName = "currency_code", nullable = false, insertable = false, updatable = false)
    private CurrencyNameEntity currencyNames;

    @Id
    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "exchange_rate", nullable = false)
    private BigDecimal exchangeRate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}