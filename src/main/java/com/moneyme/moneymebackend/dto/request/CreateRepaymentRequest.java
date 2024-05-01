package com.moneyme.moneymebackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moneyme.moneymebackend.dto.model.RepaymentRequestModel;
import lombok.Data;

@Data
public class CreateRepaymentRequest {
    @JsonProperty("group_id") String groupId;
    @JsonProperty("repayment_info") RepaymentRequestModel repaymentRequestModel;
}
