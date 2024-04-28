package com.moneyme.moneymebackend.dto.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CreateOrGetGroupResponse {
    @JsonProperty("group_info")
    GroupResponse groupInfo;

    @JsonProperty("users_info")
    List<UserResponse> userInfo;
}