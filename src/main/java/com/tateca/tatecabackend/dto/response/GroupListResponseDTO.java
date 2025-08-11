package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.GroupEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupListResponseDTO {
    @JsonProperty("group_list")
    List<GroupInfoDTO> groupList;

    public static GroupListResponseDTO from(List<GroupEntity> groupEntityList) {
        return new GroupListResponseDTO(groupEntityList.stream().map(GroupInfoDTO::from).toList());
    }
}
