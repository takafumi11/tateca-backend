package com.tateca.tatecabackend.service.impl;

import com.tateca.tatecabackend.accessor.CurrencyNameAccessor;
import com.tateca.tatecabackend.accessor.ExchangeRateAccessor;
import com.tateca.tatecabackend.api.client.ExchangeRateApiClient;
import com.tateca.tatecabackend.api.response.ExchangeRateClientResponse;
import com.tateca.tatecabackend.entity.CurrencyNameEntity;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.service.ExchangeRateUpdateService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExchangeRateUpdateServiceImpl implements ExchangeRateUpdateService {
    private static final Logger logger = LoggerFactory.getLogger(ExchangeRateUpdateServiceImpl.class);

    private final ExchangeRateAccessor exchangeRateAccessor;
    private final CurrencyNameAccessor currencyNameAccessor;
    private final ExchangeRateApiClient exchangeRateApiClient;

    @Override
    @Transactional
    public int fetchAndStoreLatestExchangeRate() {
        ExchangeRateClientResponse exchangeRateClientResponse =
                exchangeRateApiClient.fetchLatestExchangeRate();

        // Store rates for today
        LocalDate today = LocalDate.now();
        List<ExchangeRateEntity> todayEntities =
                updateExchangeRateEntities(exchangeRateClientResponse, today);

        // Store rates for tomorrow
        LocalDate tomorrow = today.plusDays(1);
        List<ExchangeRateEntity> tomorrowEntities =
                updateExchangeRateEntities(exchangeRateClientResponse, tomorrow);

        // Combine and save all entities
        List<ExchangeRateEntity> allEntities = new ArrayList<>();
        allEntities.addAll(todayEntities);
        allEntities.addAll(tomorrowEntities);

        exchangeRateAccessor.saveAll(allEntities);

        logger.info("Stored exchange rates: {} for today ({}), {} for tomorrow ({})",
                todayEntities.size(), today, tomorrowEntities.size(), tomorrow);

        return allEntities.size();
    }

    private List<ExchangeRateEntity> updateExchangeRateEntities(
            ExchangeRateClientResponse exchangeRateClientResponse,
            LocalDate date) {

        List<ExchangeRateEntity> exchangeRateEntities = new ArrayList<>();
        List<String> currencyCodes = new ArrayList<>(exchangeRateClientResponse.getConversionRates().keySet());

        // Build Maps for O(1) lookup
        Map<String, CurrencyNameEntity> currencyNameMap = buildCurrencyNameMap(currencyCodes);
        Map<String, ExchangeRateEntity> existingRatesMap = buildExistingRatesMap(currencyCodes, date);

        // Process each exchange rate
        exchangeRateClientResponse.getConversionRates().forEach((currencyCode, exchangeRate) -> {
            CurrencyNameEntity currencyNameEntity = currencyNameMap.get(currencyCode);

            if (currencyNameEntity == null) {
                logger.warn("Currency not found: {}", currencyCode);
                return;
            }

            ExchangeRateEntity exchangeRateEntity = existingRatesMap.get(currencyCode);

            if (exchangeRateEntity != null) {
                updateExistingRate(exchangeRateEntity, exchangeRate);
            } else {
                exchangeRateEntity = createNewRate(currencyNameEntity, date, exchangeRate);
            }

            exchangeRateEntities.add(exchangeRateEntity);
        });

        return exchangeRateEntities;
    }

    /**
     * Builds a map from currency code to CurrencyNameEntity for O(1) lookup
     */
    private Map<String, CurrencyNameEntity> buildCurrencyNameMap(List<String> currencyCodes) {
        List<CurrencyNameEntity> currencyNameEntities = currencyNameAccessor.findAllById(currencyCodes);
        return currencyNameEntities.stream()
                .collect(Collectors.toMap(
                        CurrencyNameEntity::getCurrencyCode,
                        Function.identity()
                ));
    }

    /**
     * Builds a map of existing exchange rates to avoid N+1 query problem
     */
    private Map<String, ExchangeRateEntity> buildExistingRatesMap(List<String> currencyCodes, LocalDate date) {
        List<ExchangeRateEntity> existingRates =
                exchangeRateAccessor.findByCurrencyCodeInAndDate(currencyCodes, date);
        return existingRates.stream()
                .collect(Collectors.toMap(
                        ExchangeRateEntity::getCurrencyCode,
                        Function.identity()
                ));
    }

    /**
     * Updates an existing exchange rate entity
     * Skips update if the rate is unchanged
     * Note: updatedAt is automatically set by @PreUpdate
     */
    private void updateExistingRate(ExchangeRateEntity entity, Double newRate) {
        BigDecimal newRateValue = BigDecimal.valueOf(newRate);

        // Skip update if rate is unchanged
        if (entity.getExchangeRate().compareTo(newRateValue) == 0) {
            return;
        }

        entity.setExchangeRate(newRateValue);
        // updatedAt is automatically set by @PreUpdate, so no manual setting is needed
    }

    /**
     * Creates a new exchange rate entity
     * Note: createdAt and updatedAt are automatically set by @PrePersist
     */
    private ExchangeRateEntity createNewRate(
            CurrencyNameEntity currencyNameEntity,
            LocalDate date,
            Double rate) {
        return ExchangeRateEntity.builder()
                .currencyCode(currencyNameEntity.getCurrencyCode())
                .date(date)
                .currencyName(currencyNameEntity)
                .exchangeRate(BigDecimal.valueOf(rate))
                // createdAt and updatedAt are automatically set by @PrePersist, so no manual setting is needed
                .build();
    }
}
