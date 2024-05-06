package com.moneyme.moneymebackend.dto.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public class LoanCreationResponse {
    @JsonProperty("loan")
    LoanResponseDTO loanResponseDTO;
    @JsonProperty("obligations")
    List<ObligationResponseDTO> obligationResponseDTOS;
}
