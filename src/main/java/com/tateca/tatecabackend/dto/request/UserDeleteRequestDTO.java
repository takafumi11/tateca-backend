package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UserDeleteRequestDTO {
    @JsonProperty("email") String email;
    @JsonProperty("uid") String uid;
}
