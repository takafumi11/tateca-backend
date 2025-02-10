package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RepaymentCreationRequest {
    @JsonProperty("repayment")
    RepaymentRequestDTO repaymentRequestDTO;
}
