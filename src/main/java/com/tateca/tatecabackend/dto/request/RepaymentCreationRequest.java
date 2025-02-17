package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

@Data
public class RepaymentCreationRequest {
    @JsonProperty("recipient_id")
    UUID recipientId;
}
