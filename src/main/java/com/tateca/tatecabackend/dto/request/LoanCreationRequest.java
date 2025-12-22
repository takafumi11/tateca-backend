package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Loan details for transaction creation")
public class LoanCreationRequest {
    @JsonProperty("obligations")
    @Schema(description = "List of loan obligations specifying who owes what amount")
    List<ObligationRequestDTO> obligationRequestDTOs;
}
