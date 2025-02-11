package com.tateca.tatecabackend.scheduler;

import com.tateca.tatecabackend.dto.response.ExchangeRateResponse;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.repository.ExchangeRateRepository;
import com.tateca.tatecabackend.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExchangeRateScheduler {
    private final ExchangeRateService exchangeRateService;
    private final ExchangeRateRepository exchangeRateRepository;

    @Scheduled(cron = "0 0 3 * * ?")  // 毎日午前3時に実行
    public void fetchAndStoreExchangeRate() {
        ExchangeRateResponse exchangeRateResponse = exchangeRateService.fetchExchangeRate();

        ExchangeRateEntity exchangeRate = new ExchangeRateEntity();
        exchangeRate.setConversionRates(exchangeRateResponse.getConversionRates());
        exchangeRateRepository.save(exchangeRate);
    }
}