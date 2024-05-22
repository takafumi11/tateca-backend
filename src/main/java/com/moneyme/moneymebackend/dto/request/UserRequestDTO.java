package com.moneyme.moneymebackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UserRequestDTO {
    @JsonProperty("uid") String uid;
    @JsonProperty("user_name") String userName;
    @JsonProperty("email") String email;
}
