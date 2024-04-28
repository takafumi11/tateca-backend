package com.moneyme.moneymebackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.UUID;

@Builder
public class CreateUserResponse {
    @JsonProperty("uuid") String uuid;
    @JsonProperty("user_name") String userName;
    @JsonProperty("email") String email;
    @JsonProperty("is_temporary") boolean isTemporary;
    @JsonProperty("created_at") String createdAt;
    @JsonProperty("updated_at") String updatedAt;
}
