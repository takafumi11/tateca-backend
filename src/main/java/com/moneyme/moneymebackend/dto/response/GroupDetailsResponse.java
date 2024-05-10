package com.moneyme.moneymebackend.dto.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GroupDetailsResponse {
    @JsonProperty("group")
    GroupResponseDTO groupResponseDTO;

    @JsonProperty("users")
    List<UserResponseDTO> userResponseDTOS;
}