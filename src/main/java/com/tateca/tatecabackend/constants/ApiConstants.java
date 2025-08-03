package com.tateca.tatecabackend.constants;

public class ApiConstants {
    // Header
    public static final String X_UID_HEADER = "x-uid";

    // Path
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String PATH_AUTH_USERS = "/auth/users";
    public static final String PATH_USERS = "/users";
    public static final String PATH_GROUPS = "/groups";
    public static final String PATH_TRANSACTIONS = "/transactions";
    public static final String PATH_LAMBDA = "/lambda";
    public static final String PATH_HISTORY = "/history";
    public static final String PATH_SETTLEMENT = "/settlement";
    public static final String PATH_EXCHANGE_RATE = "/exchange-rate";
    public static final String PATH_DEV = "/dev";

    public static final String EXCHANGE_LATEST_RATE_API_URL = "https://v6.exchangerate-api.com/v6/{api_key}/latest/JPY";
    public static final String EXCHANGE_CUSTOM_DATE_RATE_API_URL = "https://v6.exchangerate-api.com/v6/{api_key}/history/JPY/{year}/{month}/{day}";
}
