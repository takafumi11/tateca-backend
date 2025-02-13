package com.tateca.tatecabackend.service.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class TimeHelper {
    public static final ZoneId TOKYO_ZONE_ID = ZoneId.of("Asia/Tokyo");
    public static final String UTC_STRING = "UTC";
    public static final ZoneId UTC_ZONE_ID = ZoneId.of(UTC_STRING);

    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX");

    public static String convertToTokyoTime(Instant instant) {
        ZonedDateTime tokyoDateTime = instant.atZone(UTC_ZONE_ID)
                .withZoneSameInstant(TOKYO_ZONE_ID);
        return tokyoDateTime.format(formatter);
    }

    public static Instant convertToUtc(String dateTimeStr) {
        return Instant.parse(dateTimeStr);
    }
}
