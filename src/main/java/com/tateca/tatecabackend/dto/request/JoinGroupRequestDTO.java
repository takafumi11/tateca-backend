package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Request to join a group using invitation token")
public record JoinGroupRequestDTO(
        @NotNull(message = "User UUID is required")
        @JsonProperty("user_uuid")
        @Schema(description = "UUID of the user joining the group", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID userUuid,

        @NotNull(message = "Join token is required")
        @JsonProperty("join_token")
        @Schema(description = "Invitation token for joining the group", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID joinToken
) {
}
