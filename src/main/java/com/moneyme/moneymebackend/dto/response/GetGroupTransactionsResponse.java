package com.moneyme.moneymebackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moneyme.moneymebackend.dto.model.GroupBalancesResponseModel;
import lombok.Builder;

import java.util.List;

@Builder
public class GroupBalancesResponse {
   @JsonProperty("balances")
   List<GroupBalancesResponseModel> balances;
}

