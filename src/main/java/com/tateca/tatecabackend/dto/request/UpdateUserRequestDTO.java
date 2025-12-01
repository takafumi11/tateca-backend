package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Pattern;

public record UpdateUserRequestDTO(
        @JsonProperty("user_name") String name,
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency code must be 3 uppercase letters")
        @JsonProperty("currency_code") String currencyCode
) {}
