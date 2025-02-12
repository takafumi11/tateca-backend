package com.tateca.tatecabackend.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExchangeRateId implements Serializable {
    private String currencyCode;
    private LocalDate date;
}
