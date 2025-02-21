package com.tateca.tatecabackend.scheduler;

import com.tateca.tatecabackend.accessor.CurrencyNameAccessor;
import com.tateca.tatecabackend.accessor.ExchangeRateAccessor;
import com.tateca.tatecabackend.api.client.ExchangeRateApiClient;
import com.tateca.tatecabackend.api.response.ExchangeRateClientResponse;
import com.tateca.tatecabackend.entity.CurrencyNameEntity;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static com.tateca.tatecabackend.service.util.TimeHelper.UTC_STRING;
import static com.tateca.tatecabackend.service.util.TimeHelper.timeStampToLocalDateInUtc;

@Service
@RequiredArgsConstructor
public class ExchangeRateScheduler {
    private final ExchangeRateAccessor exchangeRateAccessor;
    private final CurrencyNameAccessor currencyNameAccessor;
    private final ExchangeRateApiClient exchangeRateApiClient;

    @Scheduled(cron = "0 1 0 * * *", zone = UTC_STRING)

    public void fetchAndStoreExchangeRate() {
        ExchangeRateClientResponse exchangeRateClientResponse = exchangeRateApiClient.fetchLatestExchangeRate();
        LocalDate date = timeStampToLocalDateInUtc(exchangeRateClientResponse.getTimeLastUpdateUnix());
        LocalDate nextDate = date.plus(1, ChronoUnit.DAYS);

        System.out.println("date::" + date);
        System.out.println("next date::" + nextDate);

        List<ExchangeRateEntity> exchangeRateEntities = new ArrayList<>();

        updateExchangeRateEntities(exchangeRateClientResponse, exchangeRateEntities, date);
        updateExchangeRateEntities(exchangeRateClientResponse, exchangeRateEntities, nextDate);

        exchangeRateAccessor.saveAll(exchangeRateEntities);
    }

    private void updateExchangeRateEntities(ExchangeRateClientResponse exchangeRateClientResponse, List<ExchangeRateEntity> exchangeRateEntities, LocalDate date) {
        List<String> currencyCodes = new ArrayList<>(exchangeRateClientResponse.getConversionRates().keySet());

        List<CurrencyNameEntity> currencyNameEntities = currencyNameAccessor.findAllById(currencyCodes);
        exchangeRateClientResponse.getConversionRates().forEach((currencyCode, exchangeRate) -> {
            CurrencyNameEntity currencyNameEntity = currencyNameEntities.stream()
                    .filter(entity -> entity.getCurrencyCode().equals(currencyCode))
                    .findFirst()
                    .orElse(null);

            if (currencyNameEntity == null) {
                System.out.println("Currency not found: " + currencyCode);
                return;
            }

            ExchangeRateEntity exchangeRateEntity = null;

            try {
                exchangeRateEntity = exchangeRateAccessor.findByCurrencyCodeAndDate(currencyCode, date);

                exchangeRateEntity.setExchangeRate(BigDecimal.valueOf(exchangeRate));
                exchangeRateEntity.setUpdatedAt(Instant.now());
            } catch (Exception e) {
                exchangeRateEntity = ExchangeRateEntity.builder()
                        .currencyCode(currencyNameEntity.getCurrencyCode())
                        .date(date)
                        .currencyName(currencyNameEntity)
                        .exchangeRate(BigDecimal.valueOf(exchangeRate))
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();
            } finally {
                exchangeRateEntities.add(exchangeRateEntity);
            }
        });
    }
}
