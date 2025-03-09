package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UpdateUserNameRequestDTO {
    @JsonProperty("user_name") String name;
}
