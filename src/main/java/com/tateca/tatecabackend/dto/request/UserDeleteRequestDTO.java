package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserDeleteRequestDTO(
        @JsonProperty("email") String email,
        @JsonProperty("uid") String uid
) {
}
