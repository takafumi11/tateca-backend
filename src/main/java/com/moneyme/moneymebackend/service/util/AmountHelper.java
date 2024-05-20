package com.moneyme.moneymebackend.service.util;

import java.math.BigDecimal;

public class AmountHelper {
    public static BigDecimal calculateAmount(int amountInt, BigDecimal currencyRate) {
        if (amountInt == 0) {
            return BigDecimal.ZERO;
        } else {
            return BigDecimal.valueOf(amountInt).divide(currencyRate, 6, BigDecimal.ROUND_HALF_UP);
        }
    }
}
