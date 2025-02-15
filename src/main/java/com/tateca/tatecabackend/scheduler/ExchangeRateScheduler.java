package com.tateca.tatecabackend.scheduler;

import com.tateca.tatecabackend.accessor.CurrencyNameAccessor;
import com.tateca.tatecabackend.accessor.ExchangeRateAccessor;
import com.tateca.tatecabackend.api.client.ExchangeRateApiClient;
import com.tateca.tatecabackend.api.response.ExchangeRateResponse;
import com.tateca.tatecabackend.entity.CurrencyNameEntity;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.exception.GlobalExceptionHandler;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.core.Local;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.tateca.tatecabackend.service.util.TimeHelper.UTC_STRING;
import static com.tateca.tatecabackend.service.util.TimeHelper.UTC_ZONE_ID;
import static com.tateca.tatecabackend.service.util.TimeHelper.timeStampToLocalDateInUtc;

@Service
@RequiredArgsConstructor
public class ExchangeRateScheduler {
    private final ExchangeRateAccessor exchangeRateAccessor;
    private final CurrencyNameAccessor currencyNameAccessor;
    private final ExchangeRateApiClient exchangeRateApiClient;

    private static final Logger logger = LoggerFactory.getLogger(ExchangeRateScheduler.class);

    @Scheduled(cron = "0 1 0 * * *", zone = UTC_STRING)
    public void fetchAndStoreExchangeRate() {
        ExchangeRateResponse exchangeRateResponse = exchangeRateApiClient.fetchLatestExchangeRate();
        LocalDate date = timeStampToLocalDateInUtc(exchangeRateResponse.getTimeLastUpdateUnix());

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

            ExchangeRateEntity exchangeRateEntity = null;

            try {
                exchangeRateEntity= exchangeRateAccessor.findByCurrencyCodeAndDate(currencyCode, date);

                exchangeRateEntity.setExchangeRate(BigDecimal.valueOf(exchangeRate));
                exchangeRateEntity.setUpdatedAt(Instant.now());
            } catch (Exception e) {
                exchangeRateEntity = ExchangeRateEntity.builder()
                        .currencyCode(currencyNameEntity.getCurrencyCode())
                        .currencyNames(currencyNameEntity)
                        .date(date)
                        .exchangeRate(BigDecimal.valueOf(exchangeRate))
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();
            } finally {
                exchangeRateEntities.add(exchangeRateEntity);
            }
        });

        exchangeRateAccessor.saveAll(exchangeRateEntities);
    }
}
