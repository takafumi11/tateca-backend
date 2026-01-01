package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserRequestDTO(
        @JsonProperty("user_name") String userName,
        @JsonProperty("email") String email
) {
}
