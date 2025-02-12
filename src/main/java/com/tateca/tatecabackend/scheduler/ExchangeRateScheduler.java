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
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.tateca.tatecabackend.service.util.TimeHelper.UTC_STRING;
import static com.tateca.tatecabackend.service.util.TimeHelper.UTC_ZONE_ID;

@Service
@RequiredArgsConstructor
public class ExchangeRateScheduler {
    private final ExchangeRateAccessor exchangeRateAccessor;
    private final CurrencyNameAccessor currencyNameAccessor;
    private final ExchangeRateApiClient exchangeRateApiClient;

    @Scheduled(cron = "0 1 15 * * *", zone = UTC_STRING)
    public void fetchAndStoreExchangeRate() {
        LocalDate currentDate = LocalDate.now(UTC_ZONE_ID);

        ExchangeRateResponse exchangeRateResponse = exchangeRateApiClient.fetchExchangeRate();

        List<String> currencyCodes = new ArrayList<>(exchangeRateResponse.getConversionRates().keySet());

        List<CurrencyNameEntity> currencyNameEntities = currencyNameAccessor.findAllById(currencyCodes);

        List<ExchangeRateEntity> exchangeRateEntities = new ArrayList<>();

        exchangeRateResponse.getConversionRates().forEach((currencyCode, exchangeRate) -> {
            CurrencyNameEntity currencyNameEntity = currencyNameEntities.stream()
                    .filter(entity -> entity.getCurrencyCode().equals(currencyCode))
                    .findFirst()
                    .orElse(null);

            if (currencyNameEntity == null) {
                System.out.println("Currency not found: " + currencyCode);
                return;
            }

            ExchangeRateEntity exchangeRateEntity = exchangeRateAccessor.findByCurrencyCodeAndDate(currencyCode, currentDate)
                    .orElse(null);

            if (exchangeRateEntity == null) {
                exchangeRateEntity = ExchangeRateEntity.builder()
                        .currencyCode(currencyNameEntity.getCurrencyCode())
                        .currencyNames(currencyNameEntity)
                        .date(currentDate)
                        .exchangeRate(BigDecimal.valueOf(exchangeRate))
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();
            } else {
                exchangeRateEntity.setExchangeRate(BigDecimal.valueOf(exchangeRate));
                exchangeRateEntity.setUpdatedAt(Instant.now());
            }

            exchangeRateEntities.add(exchangeRateEntity);
        });

        exchangeRateAccessor.saveAll(exchangeRateEntities);
    }
}