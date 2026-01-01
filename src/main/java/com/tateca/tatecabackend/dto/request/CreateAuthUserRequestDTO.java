package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreateAuthUserRequestDTO(
        @JsonProperty("name") String name,
        @JsonProperty("email") String email
) {
}
