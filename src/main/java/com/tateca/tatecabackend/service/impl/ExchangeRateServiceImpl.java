package com.tateca.tatecabackend.service.impl;

import com.tateca.tatecabackend.accessor.CurrencyAccessor;
import com.tateca.tatecabackend.api.client.ExchangeRateApiClient;
import com.tateca.tatecabackend.api.response.ExchangeRateClientResponse;
import com.tateca.tatecabackend.dto.response.ExchangeRateResponseDTO;
import com.tateca.tatecabackend.entity.CurrencyEntity;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.repository.ExchangeRateRepository;
import com.tateca.tatecabackend.service.ExchangeRateService;
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
public class ExchangeRateServiceImpl implements ExchangeRateService {
    private static final Logger logger = LoggerFactory.getLogger(ExchangeRateServiceImpl.class);

    private final ExchangeRateRepository exchangeRateRepository;
    private final CurrencyAccessor currencyAccessor;
    private final ExchangeRateApiClient exchangeRateApiClient;

    @Override
    @Transactional(readOnly = true)
    public ExchangeRateResponseDTO getExchangeRate(LocalDate date) {
        return ExchangeRateResponseDTO.from(exchangeRateRepository.findAllActiveByDate(date));
    }

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

        // Save only new entities (existing entities are updated via Dirty Checking)
        List<ExchangeRateEntity> newEntities = new ArrayList<>();
        newEntities.addAll(todayEntities);
        newEntities.addAll(tomorrowEntities);

        if (!newEntities.isEmpty()) {
            exchangeRateRepository.saveAll(newEntities);
        }

        int totalCount = todayEntities.size() + tomorrowEntities.size();
        logger.info("Stored exchange rates: {} new for today ({}), {} new for tomorrow ({})",
                todayEntities.size(), today, tomorrowEntities.size(), tomorrow);

        return totalCount;
    }

    private List<ExchangeRateEntity> updateExchangeRateEntities(
            ExchangeRateClientResponse exchangeRateClientResponse,
            LocalDate date) {

        List<ExchangeRateEntity> newEntities = new ArrayList<>();
        List<String> currencyCodes = new ArrayList<>(exchangeRateClientResponse.conversionRates().keySet());

        // Build Maps for O(1) lookup
        Map<String, CurrencyEntity> currencyMap = buildCurrencyMap(currencyCodes);
        Map<String, ExchangeRateEntity> existingRatesMap = buildExistingRatesMap(currencyCodes, date);

        // Process each exchange rate
        for (Map.Entry<String, Double> entry : exchangeRateClientResponse.conversionRates().entrySet()) {
            String currencyCode = entry.getKey();
            Double exchangeRate = entry.getValue();

            CurrencyEntity currencyEntity = currencyMap.get(currencyCode);

            if (currencyEntity == null) {
                logger.warn("Currency not found: {}", currencyCode);
                continue;
            }

            ExchangeRateEntity existingEntity = existingRatesMap.get(currencyCode);

            if (existingEntity != null) {
                // Update existing entity (will be automatically saved via Dirty Checking)
                updateExistingRate(existingEntity, exchangeRate);
            } else {
                // Create new entity and add to list for batch insert
                ExchangeRateEntity newEntity = createNewRate(currencyEntity, date, exchangeRate);
                newEntities.add(newEntity);
            }
        }

        return newEntities;
    }

    /**
     * Builds a map from currency code to CurrencyEntity for O(1) lookup
     */
    private Map<String, CurrencyEntity> buildCurrencyMap(List<String> currencyCodes) {
        List<CurrencyEntity> currencyEntities = currencyAccessor.findAllById(currencyCodes);
        return currencyEntities.stream()
                .collect(Collectors.toMap(
                        CurrencyEntity::getCurrencyCode,
                        Function.identity()
                ));
    }

    /**
     * Builds a map of existing exchange rates to avoid N+1 query problem
     */
    private Map<String, ExchangeRateEntity> buildExistingRatesMap(List<String> currencyCodes, LocalDate date) {
        List<ExchangeRateEntity> existingRates =
                exchangeRateRepository.findByCurrencyCodeInAndDate(currencyCodes, date);

        // Mark fetched entities as not new to avoid SELECT during save
        existingRates.forEach(ExchangeRateEntity::markAsNotNew);

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
            CurrencyEntity currencyEntity,
            LocalDate date,
            Double rate) {
        return ExchangeRateEntity.builder()
                .currencyCode(currencyEntity.getCurrencyCode())
                .date(date)
                .currency(currencyEntity)
                .exchangeRate(BigDecimal.valueOf(rate))
                // createdAt and updatedAt are automatically set by @PrePersist, so no manual setting is needed
                .build();
    }
}
