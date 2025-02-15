package com.tateca.tatecabackend.entity;

import com.tateca.tatecabackend.dto.request.LoanCreationRequest;
import com.tateca.tatecabackend.dto.request.RepaymentCreationRequest;
import com.tateca.tatecabackend.model.TransactionType;
import jakarta.persistence.Column;
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
public class TransactionEntity {
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
            @JoinColumn(name = "currency_code", referencedColumnName = "currency_code", nullable = false, updatable = false),
            @JoinColumn(name = "date", referencedColumnName = "date", nullable = false, updatable = false)
    })
    private ExchangeRateEntity exchangeRate;

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
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public static TransactionEntity from(LoanCreationRequest request, UserEntity user, GroupEntity group, ExchangeRateEntity exchangeRate) {
        return TransactionEntity.builder()
                .uuid(UUID.randomUUID())
                .group(group)
                .transactionType(TransactionType.LOAN)
                .title(request.getLoanRequestDTO().getTitle())
                .amount(request.getLoanRequestDTO().getAmount())
                .exchangeRate(exchangeRate)
                .payer(user)
                .build();
    }

    public static TransactionEntity from(RepaymentCreationRequest request, UserEntity payer, GroupEntity group, ExchangeRateEntity exchangeRate) {
        return TransactionEntity.builder()
                .uuid(UUID.randomUUID())
                .group(group)
                .transactionType(TransactionType.REPAYMENT)
                .title(request.getRepaymentRequestDTO().getTitle())
                .amount(request.getRepaymentRequestDTO().getAmount())
                .exchangeRate(exchangeRate)
                .payer(payer)
                .build();
    }
}
