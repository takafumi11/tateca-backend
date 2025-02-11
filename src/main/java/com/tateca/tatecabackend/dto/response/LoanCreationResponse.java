package com.tateca.tatecabackend.dto.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.ObligationEntity;
import com.tateca.tatecabackend.entity.TransactionEntity;
import lombok.Builder;

import java.util.List;

@Builder
public class LoanCreationResponse {
    @JsonProperty("loan")
    LoanResponseDTO loanResponseDTO;
    @JsonProperty("obligations")
    List<ObligationResponseDTO> obligationResponseDTOS;

    public static LoanCreationResponse buildResponse(TransactionEntity loan, List<ObligationEntity> obligationEntityList) {
        LoanResponseDTO loanResponseDTO = LoanResponseDTO.from(loan);
        List<ObligationResponseDTO> obligationResponseDTOList = obligationEntityList.stream().map(ObligationResponseDTO::from).toList();

        return LoanCreationResponse.builder()
                .loanResponseDTO(loanResponseDTO)
                .obligationResponseDTOS(obligationResponseDTOList)
                .build();
    }
}
