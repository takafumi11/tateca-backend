package com.moneyme.moneymebackend.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;

import java.time.ZonedDateTime;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class CustomResponseStatusException extends RuntimeException {
//    private final ZonedDateTime requestTime;
    private final String apiName;
    private final String uid;
    private final String message;
    private final HttpStatus status;
}
