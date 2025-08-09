package com.tateca.tatecabackend.api.client;


import com.tateca.tatecabackend.api.response.ExchangeRateClientResponse;
import com.tateca.tatecabackend.scheduler.ExchangeRateScheduler;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import java.time.Duration;
import java.time.LocalDate;

import static com.tateca.tatecabackend.constants.ApiConstants.EXCHANGE_CUSTOM_DATE_RATE_API_URL;
import static com.tateca.tatecabackend.constants.ApiConstants.EXCHANGE_LATEST_RATE_API_URL;

@Component
@Setter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "exchange.rate")
public class ExchangeRateApiClient {
    private String apiKey;

    private static final Logger logger = LoggerFactory.getLogger(ExchangeRateScheduler.class);
    private final WebClient webClient;

    public ExchangeRateClientResponse fetchLatestExchangeRate() {
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .intervalFunction(IntervalFunction.ofExponentialBackoff(Duration.ofSeconds(1), 2))
                .build();

        Retry retry = Retry.of("fetchLatestExchangeRate", retryConfig);

        String url = EXCHANGE_LATEST_RATE_API_URL.replace("{api_key}", apiKey);

        try {
            return Retry.decorateSupplier(retry, () -> 
                webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(ExchangeRateClientResponse.class)
                    .block()
            ).get();
        } catch (WebClientException e) {
            logger.error("Failed to fetch exchange rate after retries, detail: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error occurred in scheduled task, detail: {}", e.getMessage());
            throw e;
        }
    }

    public ExchangeRateClientResponse fetchExchangeRateByDate(LocalDate date) {
        String year = String.valueOf(date.getYear());
        String month = String.valueOf(date.getMonthValue());
        String day = String.valueOf(date.getDayOfMonth());

        String url = EXCHANGE_CUSTOM_DATE_RATE_API_URL
                .replace("{api_key}", apiKey)
                .replace("{year}", year)
                .replace("{month}", month)
                .replace("{day}", day);

        try {
            return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(ExchangeRateClientResponse.class)
                .block();
        } catch (WebClientException e) {
            logger.error("Failed to fetch exchange rate, detail: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error occurred in scheduled task, detail: {}", e.getMessage());
            throw e;
        }
    }
}