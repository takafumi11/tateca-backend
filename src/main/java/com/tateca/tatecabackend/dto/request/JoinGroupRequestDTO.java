package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record JoinGroupRequestDTO(
        @NotNull(message = "User UUID is required")
        @JsonProperty("user_uuid")
        UUID userUuid,

        @NotNull(message = "Join token is required")
        @JsonProperty("join_token")
        UUID joinToken
) {
}
