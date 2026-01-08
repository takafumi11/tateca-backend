package com.tateca.tatecabackend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TimeHelper Unit Tests")
class TimeHelperTest {

    // ========================================
    // dateStringToInstant Tests
    // ========================================

    @Nested
    @DisplayName("dateStringToInstant Tests")
    class DateStringToInstantTests {

        @Test
        @DisplayName("Should convert JST (+09:00) to UTC Instant correctly")
        void shouldConvertJstToUtcInstant() {
            // Given: Date string in JST
            String jstDateStr = "2026-01-09T01:00:38+09:00";

            // When: Converting to Instant
            Instant result = TimeHelper.dateStringToInstant(jstDateStr);

            // Then: Should be converted to UTC (subtract 9 hours)
            Instant expected = Instant.parse("2026-01-08T16:00:38Z");
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should convert UTC (Z) to Instant correctly")
        void shouldConvertUtcToInstant() {
            // Given: Date string in UTC
            String utcDateStr = "2026-01-08T15:59:30Z";

            // When: Converting to Instant
            Instant result = TimeHelper.dateStringToInstant(utcDateStr);

            // Then: Should remain the same
            Instant expected = Instant.parse("2026-01-08T15:59:30Z");
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should convert EST (-05:00) to UTC Instant correctly")
        void shouldConvertEstToUtcInstant() {
            // Given: Date string in EST
            String estDateStr = "2026-01-08T10:00:00-05:00";

            // When: Converting to Instant
            Instant result = TimeHelper.dateStringToInstant(estDateStr);

            // Then: Should be converted to UTC (add 5 hours)
            Instant expected = Instant.parse("2026-01-08T15:00:00Z");
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should convert PST (-08:00) to UTC Instant correctly")
        void shouldConvertPstToUtcInstant() {
            // Given: Date string in PST
            String pstDateStr = "2026-01-08T07:00:00-08:00";

            // When: Converting to Instant
            Instant result = TimeHelper.dateStringToInstant(pstDateStr);

            // Then: Should be converted to UTC (add 8 hours)
            Instant expected = Instant.parse("2026-01-08T15:00:00Z");
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should handle timezone offsets with minutes (+05:30)")
        void shouldHandleTimezoneWithMinutes() {
            // Given: Date string with +05:30 timezone (IST)
            String istDateStr = "2026-01-08T20:30:00+05:30";

            // When: Converting to Instant
            Instant result = TimeHelper.dateStringToInstant(istDateStr);

            // Then: Should be converted to UTC (subtract 5:30)
            Instant expected = Instant.parse("2026-01-08T15:00:00Z");
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should handle same moment in time across different timezones")
        void shouldHandleSameMomentAcrossDifferentTimezones() {
            // Given: Same moment in different timezones
            String jstDateStr = "2026-01-09T01:00:00+09:00";
            String utcDateStr = "2026-01-08T16:00:00Z";
            String estDateStr = "2026-01-08T11:00:00-05:00";
            String pstDateStr = "2026-01-08T08:00:00-08:00";

            // When: Converting all to Instant
            Instant jstInstant = TimeHelper.dateStringToInstant(jstDateStr);
            Instant utcInstant = TimeHelper.dateStringToInstant(utcDateStr);
            Instant estInstant = TimeHelper.dateStringToInstant(estDateStr);
            Instant pstInstant = TimeHelper.dateStringToInstant(pstDateStr);

            // Then: All should be equal (same moment in time)
            assertThat(jstInstant).isEqualTo(utcInstant);
            assertThat(jstInstant).isEqualTo(estInstant);
            assertThat(jstInstant).isEqualTo(pstInstant);
        }
    }

    // ========================================
    // convertToLocalDateInUtc Tests
    // ========================================

    @Nested
    @DisplayName("convertToLocalDateInUtc Tests")
    class ConvertToLocalDateInUtcTests {

        @Test
        @DisplayName("Should convert JST date to correct UTC LocalDate")
        void shouldConvertJstToUtcLocalDate() {
            // Given: Date string in JST (Jan 9, 01:00 JST)
            String jstDateStr = "2026-01-09T01:00:38+09:00";

            // When: Converting to UTC LocalDate
            LocalDate result = TimeHelper.convertToLocalDateInUtc(jstDateStr);

            // Then: Should be Jan 8 in UTC (crosses date boundary)
            assertThat(result).isEqualTo(LocalDate.of(2026, 1, 8));
        }

        @Test
        @DisplayName("Should convert UTC date to UTC LocalDate")
        void shouldConvertUtcToUtcLocalDate() {
            // Given: Date string in UTC
            String utcDateStr = "2026-01-08T15:59:30Z";

            // When: Converting to UTC LocalDate
            LocalDate result = TimeHelper.convertToLocalDateInUtc(utcDateStr);

            // Then: Should be same date
            assertThat(result).isEqualTo(LocalDate.of(2026, 1, 8));
        }

        @Test
        @DisplayName("Should convert EST date to UTC LocalDate")
        void shouldConvertEstToUtcLocalDate() {
            // Given: Date string in EST (Jan 8, 20:00 EST)
            String estDateStr = "2026-01-08T20:00:00-05:00";

            // When: Converting to UTC LocalDate
            LocalDate result = TimeHelper.convertToLocalDateInUtc(estDateStr);

            // Then: Should be Jan 9 in UTC (crosses date boundary)
            assertThat(result).isEqualTo(LocalDate.of(2026, 1, 9));
        }

        @Test
        @DisplayName("Should convert PST date to UTC LocalDate")
        void shouldConvertPstToUtcLocalDate() {
            // Given: Date string in PST (Jan 8, 17:00 PST)
            String pstDateStr = "2026-01-08T17:00:00-08:00";

            // When: Converting to UTC LocalDate
            LocalDate result = TimeHelper.convertToLocalDateInUtc(pstDateStr);

            // Then: Should be Jan 9 in UTC (crosses date boundary)
            assertThat(result).isEqualTo(LocalDate.of(2026, 1, 9));
        }

        @Test
        @DisplayName("Should handle date boundary crossing correctly")
        void shouldHandleDateBoundaryCrossingCorrectly() {
            // Given: JST date just after midnight
            String jstAfterMidnight = "2026-01-09T00:30:00+09:00";
            // Given: JST date just before midnight
            String jstBeforeMidnight = "2026-01-08T23:30:00+09:00";

            // When: Converting both
            LocalDate afterMidnight = TimeHelper.convertToLocalDateInUtc(jstAfterMidnight);
            LocalDate beforeMidnight = TimeHelper.convertToLocalDateInUtc(jstBeforeMidnight);

            // Then: Both should be Jan 8 in UTC
            assertThat(afterMidnight).isEqualTo(LocalDate.of(2026, 1, 8));
            assertThat(beforeMidnight).isEqualTo(LocalDate.of(2026, 1, 8));
        }
    }

    // ========================================
    // convertToTokyoTime Tests
    // ========================================

    @Nested
    @DisplayName("convertToTokyoTime Tests")
    class ConvertToTokyoTimeTests {

        @Test
        @DisplayName("Should convert UTC Instant to Tokyo time string")
        void shouldConvertUtcInstantToTokyoTime() {
            // Given: UTC Instant
            Instant utcInstant = Instant.parse("2026-01-08T15:00:00Z");

            // When: Converting to Tokyo time
            String result = TimeHelper.convertToTokyoTime(utcInstant);

            // Then: Should be +09:00 from UTC (midnight in Tokyo)
            assertThat(result).isEqualTo("2026-01-09T00:00:00+09:00");
        }

        @Test
        @DisplayName("Should convert UTC Instant at midnight to Tokyo time")
        void shouldConvertUtcMidnightToTokyoTime() {
            // Given: UTC Instant at midnight
            Instant utcMidnight = Instant.parse("2026-01-08T00:00:00Z");

            // When: Converting to Tokyo time
            String result = TimeHelper.convertToTokyoTime(utcMidnight);

            // Then: Should be 09:00 in Tokyo
            assertThat(result).isEqualTo("2026-01-08T09:00:00+09:00");
        }

        @Test
        @DisplayName("Should preserve correct timezone offset in output")
        void shouldPreserveCorrectTimezoneOffset() {
            // Given: Multiple UTC Instants
            Instant instant1 = Instant.parse("2026-01-08T12:00:00Z");
            Instant instant2 = Instant.parse("2026-06-15T12:00:00Z");

            // When: Converting to Tokyo time
            String result1 = TimeHelper.convertToTokyoTime(instant1);
            String result2 = TimeHelper.convertToTokyoTime(instant2);

            // Then: Both should have +09:00 offset (Tokyo doesn't have DST)
            assertThat(result1).endsWith("+09:00");
            assertThat(result2).endsWith("+09:00");
        }
    }

    // ========================================
    // timeStampToLocalDateInUtc Tests
    // ========================================

    @Nested
    @DisplayName("timeStampToLocalDateInUtc Tests")
    class TimeStampToLocalDateInUtcTests {

        @Test
        @DisplayName("Should convert Unix timestamp to UTC LocalDate")
        void shouldConvertUnixTimestampToUtcLocalDate() {
            // Given: Unix timestamp for 2026-01-08 15:00:00 UTC
            String timestamp = "1767884400"; // 2026-01-08T15:00:00Z

            // When: Converting to UTC LocalDate
            LocalDate result = TimeHelper.timeStampToLocalDateInUtc(timestamp);

            // Then: Should be Jan 8, 2026
            assertThat(result).isEqualTo(LocalDate.of(2026, 1, 8));
        }

        @Test
        @DisplayName("Should handle timestamp at midnight UTC")
        void shouldHandleTimestampAtMidnightUtc() {
            // Given: Unix timestamp for 2026-01-08 00:00:00 UTC
            String timestamp = "1767830400"; // 2026-01-08T00:00:00Z

            // When: Converting to UTC LocalDate
            LocalDate result = TimeHelper.timeStampToLocalDateInUtc(timestamp);

            // Then: Should be Jan 8, 2026
            assertThat(result).isEqualTo(LocalDate.of(2026, 1, 8));
        }

        @Test
        @DisplayName("Should handle timestamp just before midnight UTC")
        void shouldHandleTimestampJustBeforeMidnightUtc() {
            // Given: Unix timestamp for 2026-01-08 23:59:59 UTC
            String timestamp = "1767916799"; // 2026-01-08T23:59:59Z

            // When: Converting to UTC LocalDate
            LocalDate result = TimeHelper.timeStampToLocalDateInUtc(timestamp);

            // Then: Should still be Jan 8, 2026
            assertThat(result).isEqualTo(LocalDate.of(2026, 1, 8));
        }
    }
}
