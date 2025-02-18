package com.tateca.tatecabackend.dto.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.CurrencyNameEntity;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.entity.GroupEntity;
import com.tateca.tatecabackend.entity.UserEntity;
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

    @JsonProperty("currencies")
    List<CurrencyResponseDTO> currencyResponseDTOS;

    static public GroupDetailsResponse from(List<UserEntity> userEntityList, GroupEntity groupEntity, List<ExchangeRateEntity> exchangeRateEntityList) {
        return GroupDetailsResponse.builder()
                .groupResponseDTO(GroupResponseDTO.from(groupEntity))
                .userResponseDTOS(userEntityList.stream().map(UserResponseDTO::from).toList())
                .currencyResponseDTOS(exchangeRateEntityList.stream().map(CurrencyResponseDTO::from).toList())
                .build();
    }
}