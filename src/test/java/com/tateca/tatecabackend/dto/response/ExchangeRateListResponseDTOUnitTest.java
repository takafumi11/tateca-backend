package com.tateca.tatecabackend.dto.response;

import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExchangeRateListResponseDTO Unit Tests")
class ExchangeRateListResponseDTOUnitTest {

    @Test
    @DisplayName("from() should convert list of ExchangeRateEntity to DTO correctly")
    void fromShouldConvertListCorrectly() {
        // Given
        LocalDate date = LocalDate.of(2024, 1, 1);
        List<ExchangeRateEntity> entities = List.of(
                TestFixtures.ExchangeRates.jpy(date),
                TestFixtures.ExchangeRates.usd(date, new BigDecimal("0.0067")),
                TestFixtures.ExchangeRates.eur(date, new BigDecimal("0.0061"))
        );

        // When
        ExchangeRateListResponseDTO result = ExchangeRateListResponseDTO.from(entities);

        // Then
        assertThat(result.getExchangeRateResponseList()).hasSize(3);
        assertThat(result.getExchangeRateResponseList().get(0).getCurrencyCode()).isEqualTo("JPY");
        assertThat(result.getExchangeRateResponseList().get(1).getCurrencyCode()).isEqualTo("USD");
        assertThat(result.getExchangeRateResponseList().get(2).getCurrencyCode()).isEqualTo("EUR");
    }

    @Test
    @DisplayName("from() should return empty list when input is empty")
    void fromShouldReturnEmptyListWhenInputEmpty() {
        // Given
        List<ExchangeRateEntity> entities = List.of();

        // When
        ExchangeRateListResponseDTO result = ExchangeRateListResponseDTO.from(entities);

        // Then
        assertThat(result.getExchangeRateResponseList()).isEmpty();
    }

    @Test
    @DisplayName("from() should handle single element list")
    void fromShouldHandleSingleElementList() {
        // Given
        LocalDate date = LocalDate.of(2024, 1, 1);
        List<ExchangeRateEntity> entities = List.of(
                TestFixtures.ExchangeRates.jpy(date)
        );

        // When
        ExchangeRateListResponseDTO result = ExchangeRateListResponseDTO.from(entities);

        // Then
        assertThat(result.getExchangeRateResponseList()).hasSize(1);
        assertThat(result.getExchangeRateResponseList().getFirst().getCurrencyCode()).isEqualTo("JPY");
        assertThat(result.getExchangeRateResponseList().getFirst().getExchangeRate()).isEqualTo("1");
    }

    @Test
    @DisplayName("from() should preserve order of entities")
    void fromShouldPreserveOrder() {
        // Given
        LocalDate date = LocalDate.of(2024, 1, 1);
        List<ExchangeRateEntity> entities = List.of(
                TestFixtures.ExchangeRates.eur(date, new BigDecimal("0.0061")),
                TestFixtures.ExchangeRates.jpy(date),
                TestFixtures.ExchangeRates.usd(date, new BigDecimal("0.0067"))
        );

        // When
        ExchangeRateListResponseDTO result = ExchangeRateListResponseDTO.from(entities);

        // Then
        assertThat(result.getExchangeRateResponseList()).hasSize(3);
        assertThat(result.getExchangeRateResponseList().get(0).getCurrencyCode()).isEqualTo("EUR");
        assertThat(result.getExchangeRateResponseList().get(1).getCurrencyCode()).isEqualTo("JPY");
        assertThat(result.getExchangeRateResponseList().get(2).getCurrencyCode()).isEqualTo("USD");
    }

    @Test
    @DisplayName("from() should correctly map exchange rates for all entities")
    void fromShouldCorrectlyMapExchangeRates() {
        // Given
        LocalDate date = LocalDate.of(2024, 1, 1);
        List<ExchangeRateEntity> entities = List.of(
                TestFixtures.ExchangeRates.jpy(date),
                TestFixtures.ExchangeRates.usd(date, new BigDecimal("0.0067"))
        );

        // When
        ExchangeRateListResponseDTO result = ExchangeRateListResponseDTO.from(entities);

        // Then
        assertThat(result.getExchangeRateResponseList().get(0).getExchangeRate()).isEqualTo("1");
        assertThat(result.getExchangeRateResponseList().get(1).getExchangeRate()).isEqualTo("0.0067");
    }
}
