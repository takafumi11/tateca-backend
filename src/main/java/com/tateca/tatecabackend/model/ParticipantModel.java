package com.tateca.tatecabackend.model;

import com.tateca.tatecabackend.dto.response.UserInfoDTO;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@AllArgsConstructor
@Data
public class ParticipantModel implements Comparable<ParticipantModel> {
    private UserInfoDTO userId;
    private BigDecimal amount;

    @Override
    public int compareTo(ParticipantModel other) {
        return this.amount.compareTo(other.amount);
    }
}
