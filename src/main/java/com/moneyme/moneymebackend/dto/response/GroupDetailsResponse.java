package com.moneyme.moneymebackend.dto.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GroupDetailsResponse {
    @JsonProperty("group_info")
    GroupResponseDTO groupResponseDTO;

    @JsonProperty("users_info")
    List<UserResponseDTO> userResponseDTOS;
}