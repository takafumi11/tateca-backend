package com.moneyme.moneymebackend.dto.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.moneyme.moneymebackend.entity.GroupEntity;
import com.moneyme.moneymebackend.entity.UserEntity;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GroupDetailsResponse {
    @JsonProperty("group")
    GroupResponseDTO groupResponseDTO;

    @JsonProperty("users")
    List<UserResponseDTO> userResponseDTOS;

    static public GroupDetailsResponse from(List<UserEntity> userEntityList, GroupEntity groupEntity) {
        return GroupDetailsResponse.builder()
                .groupResponseDTO(GroupResponseDTO.from(groupEntity))
                .userResponseDTOS(userEntityList.stream().map(UserResponseDTO::from).toList())
                .build();
    }
}