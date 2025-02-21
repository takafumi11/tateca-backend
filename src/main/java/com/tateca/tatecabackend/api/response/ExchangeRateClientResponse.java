package com.tateca.tatecabackend.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class ExchangeRateClientResponse {
    private String result;
    @JsonProperty("time_last_update_unix")
    private String timeLastUpdateUnix;
    @JsonProperty("conversion_rates")
    private Map<String, Double> conversionRates;
}
