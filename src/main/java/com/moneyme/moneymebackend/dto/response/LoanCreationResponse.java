package com.moneyme.moneymebackend.dto.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.moneyme.moneymebackend.entity.LoanEntity;
import com.moneyme.moneymebackend.entity.ObligationEntity;
import lombok.Builder;

import java.util.List;

@Builder
public class LoanCreationResponse {
    @JsonProperty("loan")
    LoanResponseDTO loanResponseDTO;
    @JsonProperty("obligations")
    List<ObligationResponseDTO> obligationResponseDTOS;

    public static LoanCreationResponse buildResponse(LoanEntity loan, List<ObligationEntity> obligationEntityList) {
        LoanResponseDTO loanResponseDTO = LoanResponseDTO.from(loan);
        List<ObligationResponseDTO> obligationResponseDTOList = obligationEntityList.stream().map(ObligationResponseDTO::from).toList();

        return LoanCreationResponse.builder()
                .loanResponseDTO(loanResponseDTO)
                .obligationResponseDTOS(obligationResponseDTOList)
                .build();
    }
}
