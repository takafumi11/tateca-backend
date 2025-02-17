package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class LoanCreationRequest {
    @JsonProperty("obligations")
    List<ObligationRequestDTO> obligationRequestDTOs;
}
