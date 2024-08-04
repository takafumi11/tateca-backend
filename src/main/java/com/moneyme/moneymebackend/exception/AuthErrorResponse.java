package com.moneyme.moneymebackend.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class AuthErrorResponse {
    private String apiName;
    private String errorMessage;
}
