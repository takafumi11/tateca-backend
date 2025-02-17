package com.tateca.tatecabackend.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Converter(autoApply = true)
public class InstantToLocalDateConverter implements AttributeConverter<Instant, LocalDate> {

    @Override
    public LocalDate convertToDatabaseColumn(Instant attribute) {
        return attribute == null ? null : attribute.atZone(ZoneId.systemDefault()).toLocalDate();
    }

    @Override
    public Instant convertToEntityAttribute(LocalDate dbData) {
        return dbData == null ? null : dbData.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }
}
