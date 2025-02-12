package com.tateca.tatecabackend.scheduler;

import com.tateca.tatecabackend.accessor.CurrencyNameAccessor;
import com.tateca.tatecabackend.accessor.ExchangeRateAccessor;
import com.tateca.tatecabackend.api.client.ExchangeRateApiClient;
import com.tateca.tatecabackend.api.response.ExchangeRateResponse;
import com.tateca.tatecabackend.entity.CurrencyNameEntity;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExchangeRateScheduler {
    private final ExchangeRateAccessor exchangeRateAccessor;
    private final CurrencyNameAccessor currencyNameAccessor;
    private final ExchangeRateApiClient exchangeRateApiClient;

    @Scheduled(cron = "0 0 3 * * ?")
    public void fetchAndStoreExchangeRate() {
        ExchangeRateResponse exchangeRateResponse = exchangeRateApiClient.fetchExchangeRate();

        List<String> currencyCodes = new ArrayList<>(exchangeRateResponse.getConversionRates().keySet());

        List<CurrencyNameEntity> currencyNameEntities = currencyNameAccessor.findAllById(currencyCodes);

        List<ExchangeRateEntity> exchangeRateEntities = new ArrayList<>();

        exchangeRateResponse.getConversionRates().forEach((currencyCode, exchangeRate) -> {
            CurrencyNameEntity currencyNameEntity = currencyNameEntities.stream()
                    .filter(entity -> entity.getCurrencyCode().equals(currencyCode))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Currency not found: " + currencyCode));

            ExchangeRateEntity exchangeRateEntity = ExchangeRateEntity.builder()
                    .currencyNames(currencyNameEntity)
                    .date(LocalDate.now())
                    .exchangeRate(BigDecimal.valueOf(exchangeRate))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            exchangeRateEntities.add(exchangeRateEntity);
        });

        exchangeRateAccessor.saveAll(exchangeRateEntities);
    }
}