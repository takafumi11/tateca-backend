package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UserRequestDTO {
    @JsonProperty("user_name") String userName;
    @JsonProperty("email") String email;
}
