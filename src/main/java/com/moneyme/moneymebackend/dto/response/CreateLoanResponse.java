package com.moneyme.moneymebackend.dto.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.moneyme.moneymebackend.dto.model.LoanResponseModel;
import com.moneyme.moneymebackend.dto.model.ObligationResponseModel;
import lombok.Builder;

import java.util.List;

@Builder
public class CreateLoanResponse {
    @JsonProperty("loan")
    LoanResponseModel loanResponseModel;
    @JsonProperty("obligations")
    List<ObligationResponseModel> obligationResponseModels;
}
