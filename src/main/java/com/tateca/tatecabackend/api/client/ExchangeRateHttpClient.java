package com.tateca.tatecabackend.api.client;

import com.tateca.tatecabackend.api.response.ExchangeRateClientResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "/{apiKey}")
public interface ExchangeRateHttpClient {

    @GetExchange("/latest/JPY")
    ExchangeRateClientResponse fetchLatest(@PathVariable String apiKey);
}
