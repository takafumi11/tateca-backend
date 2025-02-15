package com.tateca.tatecabackend.api.client;


import com.tateca.tatecabackend.api.response.ExchangeRateResponse;
import com.google.api.client.util.Value;
import com.tateca.tatecabackend.scheduler.ExchangeRateScheduler;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

import static com.tateca.tatecabackend.constants.ApiConstants.EXCHANGE_CUSTOM_DATE_RATE_API_URL;
import static com.tateca.tatecabackend.constants.ApiConstants.EXCHANGE_LATEST_RATE_API_URL;

@Component
@Setter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "exchange.rate")
public class ExchangeRateApiClient {
    // Retrieve value from application.properties
    private String apiKey;

    private static final Logger logger = LoggerFactory.getLogger(ExchangeRateScheduler.class);
    private final RestTemplate restTemplate;

    public ExchangeRateResponse fetchLatestExchangeRate() {
        String url = EXCHANGE_LATEST_RATE_API_URL.replace("{api_key}", apiKey);
        try {
            return restTemplate.getForObject(url, ExchangeRateResponse.class);
        } catch (RestClientException e) {
            logger.error("Failed to fetch exchange rate, detail: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error occurred in scheduled task, detail: {}", e.getMessage());
            throw e;
        }
    }

    public ExchangeRateResponse fetchExchangeRateByDate(LocalDate date) {
        String year = String.valueOf(date.getYear());
        String month = String.valueOf(date.getMonthValue());
        String day = String.valueOf(date.getDayOfMonth());

        String url = EXCHANGE_CUSTOM_DATE_RATE_API_URL
                .replace("{api_key}", apiKey)
                .replace("{year}", year)
                .replace("{month}", month)
                .replace("{day}", day);


        try {
            return restTemplate.getForObject(url, ExchangeRateResponse.class);
        } catch (RestClientException e) {
            logger.error("Failed to fetch exchange rate, detail: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error occurred in scheduled task, detail: {}", e.getMessage());
            throw e;
        }
    }
}