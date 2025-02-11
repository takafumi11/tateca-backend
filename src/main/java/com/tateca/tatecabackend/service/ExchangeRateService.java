package com.tateca.tatecabackend.service;

import com.google.api.client.util.Value;
import com.tateca.tatecabackend.dto.response.ExchangeRateResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ExchangeRateService {

    private final String apiUrl = "https://v6.exchangerate-api.com/v6/{api_key}/latest/JPY";

    @Value("${exchange.rate.api.key}")
    private String apiKey; // application.propertiesにAPIキーを格納

    private final RestTemplate restTemplate;

    public ExchangeRateService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ExchangeRateResponse fetchExchangeRate() {
        String url = apiUrl.replace("{api_key}", apiKey);
        return restTemplate.getForObject(url, ExchangeRateResponse.class);
    }
}