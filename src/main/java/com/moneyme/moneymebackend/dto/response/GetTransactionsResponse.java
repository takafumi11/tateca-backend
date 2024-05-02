package com.moneyme.moneymebackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moneyme.moneymebackend.dto.model.TransactionResponseModel;
import lombok.Builder;
import lombok.Data;

import java.util.List;
@Data
@Builder
public class GetTransactionsResponse {
    @JsonProperty("transactions")
    List<TransactionResponseModel> transactions;
}