package com.tateca.tatecabackend.entity;

import com.tateca.tatecabackend.model.SymbolPosition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "currency_names")
public class CurrencyNameEntity {
    @Id
    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "jp_currency_name", nullable = false, length = 50)
    private String jpCurrencyName;

    @Column(name = "eng_currency_name", nullable = false, length = 50)
    private String engCurrencyName;

    @Column(name = "jp_country_name", nullable = false, length = 50)
    private String jpCountryName;

    @Column(name = "eng_country_name", nullable = false, length = 50)
    private String engCountryName;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "currency_symbol", length = 10)
    private String currencySymbol;

    @Column(name = "symbol_position")
    @Enumerated(EnumType.STRING)
    private SymbolPosition symbolPosition;
}
