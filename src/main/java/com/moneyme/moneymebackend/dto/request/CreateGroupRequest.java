package com.moneyme.moneymebackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class CreateGroupRequest {
    @JsonProperty("user_uuid") String userUuid;
    @JsonProperty("group_name") String groupName;
    @JsonProperty("users_name") List<String> usersName;
}
