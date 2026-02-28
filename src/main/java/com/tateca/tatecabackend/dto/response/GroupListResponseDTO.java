package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.dto.response.internal.GroupResponse;
import com.tateca.tatecabackend.entity.GroupEntity;

import java.util.List;

public record GroupListResponseDTO(
        @JsonProperty("group_list")
        List<GroupResponse> groupList
) {
    public static GroupListResponseDTO from(List<GroupEntity> groupEntityList) {
        return new GroupListResponseDTO(
                groupEntityList.stream()
                        .map(GroupResponse::from)
                        .toList()
        );
    }
}
