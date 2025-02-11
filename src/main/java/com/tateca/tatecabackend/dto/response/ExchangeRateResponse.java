package com.tateca.tatecabackend.dto.response;

import lombok.Data;

import java.util.Map;

@Data
public class ExchangeRateResponse {
    private String result;
    private Map<String, Double> conversionRates;
}
