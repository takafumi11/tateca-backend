package com.tateca.tatecabackend.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Converter(autoApply = true)
public class InstantToLocalDateTimeConverter implements AttributeConverter<Instant, LocalDateTime> {

    @Override
    public LocalDateTime convertToDatabaseColumn(Instant attribute) {
        return attribute == null ? null : LocalDateTime.ofInstant(attribute, ZoneId.systemDefault());
    }

    @Override
    public Instant convertToEntityAttribute(LocalDateTime dbData) {
        return dbData == null ? null : dbData.atZone(ZoneId.systemDefault()).toInstant();
    }
}
