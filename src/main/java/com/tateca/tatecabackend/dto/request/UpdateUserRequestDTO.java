package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UpdateUserRequestDTO {
    @JsonProperty("user_name") String name;
    @JsonProperty("currency_code") String currencyCode;
}
