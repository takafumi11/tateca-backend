package com.moneyme.moneymebackend.dto.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public class CreateLoanResponse {
    @JsonProperty("loan") LoanResponse loan;
    @JsonProperty("obligations") List<ObligationResponse> obligations;
}
