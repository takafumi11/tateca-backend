package com.moneyme.moneymebackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class JoinGroupRequest {
    @JsonProperty("tmp_user_id") String tmpUserId;
    @JsonProperty("actual_user_id") String actualUserId;
}
