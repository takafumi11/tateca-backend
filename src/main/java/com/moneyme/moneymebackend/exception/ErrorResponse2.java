package com.moneyme.moneymebackend.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.ZonedDateTime;

@AllArgsConstructor
@Data
public class ErrorResponse2 {
    private ZonedDateTime requestTime;
    private String apiName;
    private String uid;
    private String errorMessage;
}
