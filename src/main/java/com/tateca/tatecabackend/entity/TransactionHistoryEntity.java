package com.tateca.tatecabackend.entity;

import com.tateca.tatecabackend.model.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "transaction_history")
public class TransactionHistoryEntity {
    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID uuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_uuid", nullable = false, updatable = false)
    private GroupEntity group;

    @Column(name = "title", length = 50)
    private String title;

    @Column(name = "amount", nullable = false)
    private int amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "currency_code", referencedColumnName = "currency_code", nullable = false),
            @JoinColumn(name = "exchange_rate_date", referencedColumnName = "date", nullable = false)
    })
    private ExchangeRateEntity exchangeRate;

    @Column(name = "transaction_date", nullable = false)
    @Convert(converter = InstantToLocalDateTimeConverter.class)
    private Instant transactionDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id", nullable = false)
    private UserEntity payer;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public static TransactionHistoryEntity from(
            TransactionType transactionType,
            String title,
            int amount,
            Instant transactionDate,
            UserEntity payer,
            GroupEntity group,
            ExchangeRateEntity exchangeRate) {
        return TransactionHistoryEntity.builder()
                .uuid(UUID.randomUUID())
                .group(group)
                .transactionType(transactionType)
                .title(title)
                .amount(amount)
                .transactionDate(transactionDate)
                .exchangeRate(exchangeRate)
                .payer(payer)
                .build();
    }
}
