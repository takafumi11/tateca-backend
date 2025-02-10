package com.tateca.tatecabackend.service.util;

import java.math.BigDecimal;

public class AmountHelper {
    public static BigDecimal calculateAmount(int amountInt, BigDecimal currencyRate) {
        System.out.println("amount" + amountInt);
        System.out.println("rate" + currencyRate);
        return BigDecimal.valueOf(amountInt).multiply(currencyRate);
    }
}
