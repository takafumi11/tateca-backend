package com.moneyme.moneymebackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moneyme.moneymebackend.dto.model.GroupTransactionsResponseModel;
import lombok.Builder;

import java.util.List;

@Builder
public class GetGroupTransactionsResponse {
   @JsonProperty("transactions")
   List<GroupTransactionsResponseModel> transactions;
}

