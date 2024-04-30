package com.moneyme.moneymebackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moneyme.moneymebackend.dto.model.LoanInfo;
import com.moneyme.moneymebackend.dto.model.ObligationInfo;
import lombok.Data;

import java.util.List;

@Data
public class CreateLoanRequest {
    @JsonProperty("group_id") String groupId;
    @JsonProperty("loan_info") LoanInfo loanInfo;
    @JsonProperty("obligations_info") List<ObligationInfo> obligationInfoList;
}
