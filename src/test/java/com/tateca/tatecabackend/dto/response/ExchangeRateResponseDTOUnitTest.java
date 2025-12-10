package com.tateca.tatecabackend.dto.response;

import com.tateca.tatecabackend.entity.CurrencyNameEntity;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import com.tateca.tatecabackend.model.SymbolPosition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExchangeRateResponseDTO Unit Tests")
class ExchangeRateResponseDTOUnitTest {

    @Test
    @DisplayName("from() should convert ExchangeRateEntity to DTO correctly")
    void fromShouldConvertExchangeRateEntityCorrectly() {
        // Given
        LocalDate date = LocalDate.of(2024, 1, 1);
        ExchangeRateEntity entity = TestFixtures.ExchangeRates.jpy(date);

        // When
        ExchangeRateResponseDTO result = ExchangeRateResponseDTO.from(entity);

        // Then
        assertThat(result.getCurrencyCode()).isEqualTo("JPY");
        assertThat(result.getJpCurrencyName()).isEqualTo("日本円");
        assertThat(result.getEngCurrencyName()).isEqualTo("Japanese Yen");
        assertThat(result.getJpCountryName()).isEqualTo("日本");
        assertThat(result.getEngCountryName()).isEqualTo("Japan");
        assertThat(result.getCurrencySymbol()).isEqualTo("¥");
        assertThat(result.getSymbolPosition()).isEqualTo(SymbolPosition.PREFIX);
        assertThat(result.getExchangeRate()).isEqualTo("1");
    }

    @Test
    @DisplayName("from() should convert USD exchange rate correctly")
    void fromShouldConvertUsdExchangeRateCorrectly() {
        // Given
        LocalDate date = LocalDate.of(2024, 1, 1);
        BigDecimal rate = new BigDecimal("0.0067");
        ExchangeRateEntity entity = TestFixtures.ExchangeRates.usd(date, rate);

        // When
        ExchangeRateResponseDTO result = ExchangeRateResponseDTO.from(entity);

        // Then
        assertThat(result.getCurrencyCode()).isEqualTo("USD");
        assertThat(result.getJpCurrencyName()).isEqualTo("米ドル");
        assertThat(result.getEngCurrencyName()).isEqualTo("US Dollar");
        assertThat(result.getJpCountryName()).isEqualTo("アメリカ");
        assertThat(result.getEngCountryName()).isEqualTo("United States");
        assertThat(result.getCurrencySymbol()).isEqualTo("$");
        assertThat(result.getSymbolPosition()).isEqualTo(SymbolPosition.PREFIX);
        assertThat(result.getExchangeRate()).isEqualTo("0.0067");
    }

    @Test
    @DisplayName("from() should handle decimal exchange rate with many digits")
    void fromShouldHandleDecimalExchangeRate() {
        // Given
        LocalDate date = LocalDate.of(2024, 1, 1);
        BigDecimal rate = new BigDecimal("0.00689655172");
        ExchangeRateEntity entity = TestFixtures.ExchangeRates.usd(date, rate);

        // When
        ExchangeRateResponseDTO result = ExchangeRateResponseDTO.from(entity);

        // Then
        assertThat(result.getExchangeRate()).isEqualTo("0.00689655172");
    }

    @Test
    @DisplayName("from() should map all CurrencyNameEntity fields correctly")
    void fromShouldMapAllCurrencyNameFieldsCorrectly() {
        // Given
        LocalDate date = LocalDate.of(2024, 1, 1);
        ExchangeRateEntity entity = TestFixtures.ExchangeRates.eur(date, new BigDecimal("0.0061"));

        // When
        ExchangeRateResponseDTO result = ExchangeRateResponseDTO.from(entity);

        // Then
        assertThat(result.getCurrencyCode()).isEqualTo("EUR");
        assertThat(result.getJpCurrencyName()).isEqualTo("ユーロ");
        assertThat(result.getEngCurrencyName()).isEqualTo("Euro");
        assertThat(result.getJpCountryName()).isEqualTo("欧州");
        assertThat(result.getEngCountryName()).isEqualTo("Europe");
        assertThat(result.getCurrencySymbol()).isEqualTo("€");
    }

    @Test
    @DisplayName("from() should handle custom currency with SUFFIX symbol position")
    void fromShouldHandleSuffixSymbolPosition() {
        // Given
        LocalDate date = LocalDate.of(2024, 1, 1);
        CurrencyNameEntity currency = CurrencyNameEntity.builder()
                .currencyCode("KRW")
                .jpCurrencyName("韓国ウォン")
                .engCurrencyName("Korean Won")
                .jpCountryName("韓国")
                .engCountryName("South Korea")
                .isActive(true)
                .currencySymbol("₩")
                .symbolPosition(SymbolPosition.SUFFIX)
                .build();
        ExchangeRateEntity entity = TestFixtures.ExchangeRates.withCurrency(date, currency, new BigDecimal("9.0"));

        // When
        ExchangeRateResponseDTO result = ExchangeRateResponseDTO.from(entity);

        // Then
        assertThat(result.getCurrencyCode()).isEqualTo("KRW");
        assertThat(result.getSymbolPosition()).isEqualTo(SymbolPosition.SUFFIX);
        assertThat(result.getExchangeRate()).isEqualTo("9.0");
    }
}
