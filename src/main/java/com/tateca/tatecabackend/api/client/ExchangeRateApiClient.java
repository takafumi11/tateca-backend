package com.tateca.tatecabackend.api.client;


import com.tateca.tatecabackend.api.response.ExchangeRateResponse;
import com.google.api.client.util.Value;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class ExchangeRateApiClient {

    private final String apiUrl = "https://v6.exchangerate-api.com/v6/{api_key}/latest/JPY";
//    @Value("${exchange.rate.api.key}")
    private String apiKey = "384ca405f0f7a7d19e60ec62";

    private final RestTemplate restTemplate;

    public ExchangeRateResponse fetchExchangeRate() {
        String url = apiUrl.replace("{api_key}", apiKey);
        try {
            return restTemplate.getForObject(url, ExchangeRateResponse.class);
        } catch (Exception e) {
            System.out.println("error:" + e);
            throw e;
        }
    }
}