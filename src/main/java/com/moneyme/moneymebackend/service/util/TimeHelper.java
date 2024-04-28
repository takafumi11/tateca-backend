package com.moneyme.moneymebackend.service.util;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class TimeHelper {
    private static final ZoneId TOKYO_ZONE_ID = ZoneId.of("Asia/Tokyo");
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;

    public static ZonedDateTime getCurrentTimeInTokyo() {
        return ZonedDateTime.now(TOKYO_ZONE_ID);
    }

    public static String convertToTokyoTime(ZonedDateTime dateTime) {
        return dateTime.withZoneSameInstant(TOKYO_ZONE_ID).format(formatter);
    }

}
