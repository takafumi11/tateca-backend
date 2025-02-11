package com.tateca.tatecabackend.scheduler;

import com.tateca.tatecabackend.api.client.ExchangeRateApiClient;
import com.tateca.tatecabackend.api.response.ExchangeRateResponse;
import com.tateca.tatecabackend.entity.CurrencyNameEntity;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.repository.CurrencyNameRepository;
import com.tateca.tatecabackend.repository.ExchangeRateRepository;
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
    // TODO: replace with accessor
    private final ExchangeRateRepository exchangeRateRepository;
    private final CurrencyNameRepository currencyNameRepository;
    private final ExchangeRateApiClient exchangeRateApiClient;

    @Scheduled(cron = "0 0 3 * * ?")
    public void fetchAndStoreExchangeRate() {
        ExchangeRateResponse exchangeRateResponse = exchangeRateApiClient.fetchExchangeRate();

        List<String> currencyCodes = new ArrayList<>(exchangeRateResponse.getConversionRates().keySet());

        List<CurrencyNameEntity> currencyNameEntities = currencyNameRepository.findAllById(currencyCodes);

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

        exchangeRateRepository.saveAll(exchangeRateEntities);
    }
}