package com.tateca.tatecabackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "`groups`")
public class GroupEntity {
    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID uuid;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @ManyToOne
    @JoinColumn(name = "currency_code", referencedColumnName = "currency_code")
    private CurrencyNameEntity currencyName;

    @Column(name = "join_token", nullable = false, columnDefinition = "BINARY(16)")
    private UUID joinToken;

    @Column(name = "token_expires", nullable = false)
    private Instant tokenExpires;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        // TODO: now + 1
        Instant now = Instant.now();
        tokenExpires = now.plus(1, ChronoUnit.DAYS);
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
