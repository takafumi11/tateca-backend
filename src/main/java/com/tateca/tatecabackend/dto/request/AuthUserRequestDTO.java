package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthUserRequestDTO(
        @JsonProperty("name") String name,
        @JsonProperty("email") String email
) {
}
