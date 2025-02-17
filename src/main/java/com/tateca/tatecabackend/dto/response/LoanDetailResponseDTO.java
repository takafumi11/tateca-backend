package com.tateca.tatecabackend.dto.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.TransactionObligationEntity;
import lombok.Builder;

import java.util.List;

@Builder
public class LoanDetailResponseDTO {
    @JsonProperty("obligations")
    List<LoanObligationResponseDTO> loanObligationResponseDTOs;

    public static LoanDetailResponseDTO from(List<TransactionObligationEntity> transactionObligationEntityList) {
        List<LoanObligationResponseDTO> loanObligationResponseDTOList = transactionObligationEntityList.stream().map(LoanObligationResponseDTO::from).toList();

        return LoanDetailResponseDTO.builder()
                .loanObligationResponseDTOs(loanObligationResponseDTOList)
                .build();
    }
}
