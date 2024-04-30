package com.moneyme.moneymebackend.dto.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.moneyme.moneymebackend.dto.model.GroupResponseModel;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserGroupsResponse {
    @JsonProperty("group_info")
    GroupResponseModel groupResponseModel;

    @JsonProperty("users_info")
    List<UserResponse> userResponses;
}