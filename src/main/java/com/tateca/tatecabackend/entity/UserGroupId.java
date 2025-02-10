package com.tateca.tatecabackend.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class UserGroupId implements Serializable {
    private UUID userUuid;
    private UUID groupUuid;
}