package com.tateca.tatecabackend.service.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class TimeHelper {
    public static final ZoneId TOKYO_ZONE_ID = ZoneId.of("Asia/Tokyo");
    public static final String UTC_STRING = "UTC";
    public static final ZoneId UTC_ZONE_ID = ZoneId.of(UTC_STRING);

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX");

    public static String convertToTokyoTime(Instant instant) {
        ZonedDateTime tokyoDateTime = instant.atZone(UTC_ZONE_ID)
                .withZoneSameInstant(TOKYO_ZONE_ID);
        return tokyoDateTime.format(DATE_TIME_FORMATTER);
    }

    public static String localDateToTokyoTime(LocalDate localDate) {
        LocalDateTime localDateTime = localDate.atStartOfDay();

        return localDateTime.atOffset(ZoneOffset.UTC).format(DATE_TIME_FORMATTER);
    }

    public static LocalDate timeStampToLocalDateInUtc(String timestampStr) {
        long timestamp = Long.parseLong(timestampStr);
        Instant instant = Instant.ofEpochSecond(timestamp);

        return instant.atOffset(ZoneOffset.UTC).toLocalDate();
    }

    public static LocalDate convertToLocalDateInUtc(String dateTimeStr) {
        OffsetDateTime offsetDateTime = OffsetDateTime.parse(dateTimeStr, DATE_TIME_FORMATTER);

        return offsetDateTime.withOffsetSameInstant(ZoneOffset.UTC).toLocalDate();
    }

    public static Instant convertToUtc(String dateTimeStr) {
        return Instant.parse(dateTimeStr);
    }
}
