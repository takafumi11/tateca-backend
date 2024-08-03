package com.moneyme.moneymebackend.annotation;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.ZonedDateTime;

@AllArgsConstructor
public class BearerTokenWithRequestTime {
    private final String uid;
    private final ZonedDateTime requestTime;
}