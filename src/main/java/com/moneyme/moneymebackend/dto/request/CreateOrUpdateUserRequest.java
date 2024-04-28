package com.moneyme.moneymebackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CreateOrUpdateUserRequest {
    @JsonProperty("user_name") String userName;
    @JsonProperty("email") String email;
}
