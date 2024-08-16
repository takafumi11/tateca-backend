package com.moneyme.moneymebackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class CreateGroupRequest {
    @JsonProperty("group_name") String groupName;
    @JsonProperty("host_name") String hostName;
    @JsonProperty("participants_name") List<String> participantsName;
}
