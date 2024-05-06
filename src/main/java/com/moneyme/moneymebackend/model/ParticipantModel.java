package com.moneyme.moneymebackend.model;

import com.moneyme.moneymebackend.dto.response.UserResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@AllArgsConstructor
@Data
public class ParticipantModel implements Comparable<ParticipantModel> {
    private UserResponseDTO userId;
    private BigDecimal amount;

    @Override
    public int compareTo(ParticipantModel other) {
        return this.amount.compareTo(other.amount);
    }
}
