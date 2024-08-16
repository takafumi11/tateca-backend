package com.moneyme.moneymebackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AuthUserRequestDTO {
    @JsonProperty("name") String name;
    @JsonProperty("email") String email;
}
