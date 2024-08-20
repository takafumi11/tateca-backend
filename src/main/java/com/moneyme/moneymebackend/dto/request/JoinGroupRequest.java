package com.moneyme.moneymebackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

@Data
public class JoinGroupRequest {
    @JsonProperty("user_uuid") UUID userUuid;
    @JsonProperty("join_token") UUID joinToken;
}
