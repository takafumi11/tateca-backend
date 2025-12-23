package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(description = "Repayment details for transaction creation")
public class RepaymentCreationRequest {
    @JsonProperty("recipient_id")
    @Schema(description = "UUID of the user receiving the repayment", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID recipientId;
}
