package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.accessor.CurrencyNameAccessor;
import com.tateca.tatecabackend.accessor.ExchangeRateAccessor;
import com.tateca.tatecabackend.api.client.ExchangeRateApiClient;
import com.tateca.tatecabackend.api.response.ExchangeRateClientResponse;
import com.tateca.tatecabackend.entity.CurrencyNameEntity;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
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
    public int fetchAndStoreExchangeRateByDate(LocalDate targetDate) {
        ExchangeRateClientResponse exchangeRateClientResponse =
                exchangeRateApiClient.fetchExchangeRateByDate(targetDate);

        List<ExchangeRateEntity> exchangeRateEntities =
                updateExchangeRateEntities(exchangeRateClientResponse, targetDate);

        exchangeRateAccessor.saveAll(exchangeRateEntities);

        return exchangeRateEntities.size();
    }

    private List<ExchangeRateEntity> updateExchangeRateEntities(
            ExchangeRateClientResponse exchangeRateClientResponse,
            LocalDate date) {

        List<ExchangeRateEntity> exchangeRateEntities = new ArrayList<>();
        List<String> currencyCodes = new ArrayList<>(exchangeRateClientResponse.getConversionRates().keySet());

        // O(1)ルックアップ用のMapを構築
        Map<String, CurrencyNameEntity> currencyNameMap = buildCurrencyNameMap(currencyCodes);
        Map<String, ExchangeRateEntity> existingRatesMap = buildExistingRatesMap(currencyCodes, date);

        // 各為替レートを処理
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
     * 通貨コードからCurrencyNameEntityへのMapを構築（O(1)ルックアップ用）
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
     * 既存の為替レートを一括取得してMapを構築（N+1問題を回避）
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
     * 既存の為替レートエンティティを更新
     * レートが変わっていない場合は更新をスキップする
     * Note: updatedAtは@PreUpdateによって自動的に設定される
     */
    private void updateExistingRate(ExchangeRateEntity entity, Double newRate) {
        BigDecimal newRateValue = BigDecimal.valueOf(newRate);

        // レートが変わっていない場合は更新をスキップ
        if (entity.getExchangeRate().compareTo(newRateValue) == 0) {
            return;
        }

        entity.setExchangeRate(newRateValue);
        // updatedAtは@PreUpdateによって自動的に設定されるため、手動設定は不要
    }

    /**
     * 新しい為替レートエンティティを作成
     * Note: createdAtとupdatedAtは@PrePersistによって自動的に設定される
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
                // createdAtとupdatedAtは@PrePersistによって自動的に設定されるため、手動設定は不要
                .build();
    }
}
