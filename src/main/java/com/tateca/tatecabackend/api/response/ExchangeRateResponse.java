package com.tateca.tatecabackend.api.response;

import lombok.Data;

import java.util.Map;

@Data
public class ExchangeRateResponse {
    private String result;
    private Map<String, Double> conversionRates;
}
