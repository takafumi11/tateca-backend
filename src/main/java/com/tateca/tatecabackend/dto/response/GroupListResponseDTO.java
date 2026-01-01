package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.dto.response.internal.GroupResponse;
import com.tateca.tatecabackend.entity.GroupEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "List of groups")
public record GroupListResponseDTO(
        @JsonProperty("group_list")
        @Schema(description = "List of groups the user belongs to")
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
