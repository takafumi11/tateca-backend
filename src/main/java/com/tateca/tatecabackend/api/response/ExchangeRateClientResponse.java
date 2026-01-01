package com.tateca.tatecabackend.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record ExchangeRateClientResponse(
        String result,
        @JsonProperty("time_last_update_unix")
        String timeLastUpdateUnix,
        @JsonProperty("conversion_rates")
        Map<String, Double> conversionRates
) {
}
