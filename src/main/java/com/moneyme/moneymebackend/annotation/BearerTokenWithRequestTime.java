package com.moneyme.moneymebackend.annotation;

import lombok.AllArgsConstructor;

import java.time.ZonedDateTime;

@AllArgsConstructor
public class BearerTokenWithRequestTime {
    private final String uid;
    private final ZonedDateTime requestTime;
}