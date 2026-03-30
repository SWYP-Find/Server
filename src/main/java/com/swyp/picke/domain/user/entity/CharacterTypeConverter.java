package com.swyp.picke.domain.user.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CharacterTypeConverter implements AttributeConverter<CharacterType, String> {

    @Override
    public String convertToDatabaseColumn(CharacterType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public CharacterType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : CharacterType.from(dbData);
    }
}
