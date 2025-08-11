package com.tateca.tatecabackend.dto.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.GroupEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GroupDetailsResponseDTO {
    @JsonProperty("group")
    GroupInfoDTO groupInfo;

    @JsonProperty("users")
    List<UserInfoDTO> users;

    @JsonProperty("transaction_count")
    Long transactionCount;

    static public GroupDetailsResponseDTO from(List<UserEntity> userEntityList, GroupEntity groupEntity, Long transactionCount) {
        return GroupDetailsResponseDTO.builder()
                .groupInfo(GroupInfoDTO.from(groupEntity))
                .users(userEntityList.stream().map(UserInfoDTO::from).toList())
                .transactionCount(transactionCount)
                .build();
    }
}