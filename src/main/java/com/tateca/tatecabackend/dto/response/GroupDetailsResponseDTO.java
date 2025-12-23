package com.tateca.tatecabackend.dto.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.GroupEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Detailed group information including members and statistics")
public class GroupDetailsResponseDTO {
    @JsonProperty("group")
    @Schema(description = "Group information")
    GroupInfoDTO groupInfo;

    @JsonProperty("users")
    @Schema(description = "List of users in the group")
    List<UserInfoDTO> users;

    @JsonProperty("transaction_count")
    @Schema(description = "Total number of transactions in the group", example = "42")
    Long transactionCount;

    static public GroupDetailsResponseDTO from(List<UserEntity> userEntityList, GroupEntity groupEntity, Long transactionCount) {
        return GroupDetailsResponseDTO.builder()
                .groupInfo(GroupInfoDTO.from(groupEntity))
                .users(userEntityList.stream().map(UserInfoDTO::from).toList())
                .transactionCount(transactionCount)
                .build();
    }
}