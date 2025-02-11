package com.tateca.tatecabackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(name = "jp_display_name", nullable = false, length = 50)
    private String jpDisplayName;

    @Column(name = "eng_display_name", nullable = false, length = 50)
    private String engDisplayName;

    @Column(name = "jp_country_name", nullable = false, length = 50)
    private String jpCountryName;

    @Column(name = "eng_country_name", nullable = false, length = 50)
    private String engCountryName;
}
