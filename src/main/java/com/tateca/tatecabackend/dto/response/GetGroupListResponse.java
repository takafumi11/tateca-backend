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
public class GetGroupListResponse {
    @JsonProperty("group_list")
    List<GroupResponseDTO> groupList;

    public static GetGroupListResponse from(List<GroupEntity> groupEntityList) {
        return new GetGroupListResponse(groupEntityList.stream().map(GroupResponseDTO::from).toList());
    }
}
