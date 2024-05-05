package com.moneyme.moneymebackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moneyme.moneymebackend.dto.model.LoanRequestModel;
import com.moneyme.moneymebackend.dto.model.ObligationRequestModel;
import lombok.Data;

import java.util.List;

@Data
public class CreateLoanRequest {
    @JsonProperty("loan_info")
    LoanRequestModel loanRequestModel;
    @JsonProperty("obligations_info")
    List<ObligationRequestModel> obligationRequestModels;
}
