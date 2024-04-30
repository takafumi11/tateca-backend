package com.moneyme.moneymebackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateLoanRequest {
    @JsonProperty("group_id") String groupId;
    @JsonProperty("loan_info") LoanInfo loanInfo;
    @JsonProperty("obligations_info") List<ObligationInfo> obligationInfoList;

    @Data
    public class LoanInfo {
        @JsonProperty("title") String title;
        @JsonProperty("amount")
        BigDecimal amount;
        @JsonProperty("date") String date;
        @JsonProperty("payer_id") String payerId;
        @JsonProperty("detail") String detail;
    }

    @Data
    public class ObligationInfo {
        @JsonProperty("amount") BigDecimal amount;
        @JsonProperty("user_uuid") String userUuid ;
    }
}
