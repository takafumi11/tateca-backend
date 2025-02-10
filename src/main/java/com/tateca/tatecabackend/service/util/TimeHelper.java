package com.tateca.tatecabackend.service.util;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class TimeHelper {
    private static final ZoneId TOKYO_ZONE_ID = ZoneId.of("Asia/Tokyo");
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    public static String convertToTokyoTime(ZonedDateTime dateTime) {
        return dateTime.withZoneSameInstant(TOKYO_ZONE_ID).format(formatter);
    }

    public static ZonedDateTime convertToTokyoTime(String dateTimeStr) {
        return ZonedDateTime.parse(dateTimeStr, formatter).withZoneSameInstant(TOKYO_ZONE_ID);
    }

}
