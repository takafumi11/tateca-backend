package com.tateca.tatecabackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(ExchangeRateId.class)
@Table(name = "exchange_rates")
public class ExchangeRateEntity implements Persistable<ExchangeRateId> {
    @Id
    @Column(name = "currency_code", nullable = false)
    private String currencyCode;

    @Id
    @Column(name = "date", nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_code", referencedColumnName = "currency_code", nullable = false, insertable = false, updatable = false)
    private CurrencyEntity currency;

    @Column(name = "exchange_rate", nullable = false)
    private BigDecimal exchangeRate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public ExchangeRateId getId() {
        return new ExchangeRateId(currencyCode, date);
    }

    @Override
    public boolean isNew() {
        return isNew || createdAt == null;
    }

    /**
     * Mark this entity as not new (existing in database)
     */
    public void markAsNotNew() {
        this.isNew = false;
    }
}