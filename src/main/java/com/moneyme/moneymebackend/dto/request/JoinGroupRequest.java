package com.moneyme.moneymebackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class JoinGroupRequest {
    @JsonProperty("old_user_id") String oldUserId;
    @JsonProperty("new_user_id") String newUserId;
}
