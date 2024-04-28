package com.moneyme.moneymebackend.dto.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.moneyme.moneymebackend.dto.model.GroupInfo;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CreateGroupResponse {
    @JsonProperty("group_info")
    GroupResponse groupInfo;

    @JsonProperty("users_info")
    List<UserResponse> userInfo;
}