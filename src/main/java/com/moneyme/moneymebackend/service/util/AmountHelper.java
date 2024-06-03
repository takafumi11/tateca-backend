package com.moneyme.moneymebackend.service.util;

import java.math.BigDecimal;

public class AmountHelper {
    public static BigDecimal calculateAmount(int amountInt, BigDecimal currencyRate) {
        if (amountInt == 0) {
            return BigDecimal.ZERO;
        } else {
            return BigDecimal.valueOf(amountInt).multiply(currencyRate);
        }
    }
}
