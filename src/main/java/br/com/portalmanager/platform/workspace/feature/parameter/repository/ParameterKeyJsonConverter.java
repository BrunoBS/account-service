package br.com.portalmanager.platform.workspace.feature.parameter.repository;

import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.library.messaging.message.PlatformMessageKeys;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Converter(autoApply = true)
public class ParameterKeyJsonConverter implements AttributeConverter<JsonNode, String> {
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    @Override
    public String convertToDatabaseColumn(JsonNode value) {
        return value == null ? null : MAPPER.writeValueAsString(value);
    }

    @Override
    public JsonNode convertToEntityAttribute(String value) {
        if (value == null) return null;
        try {
            return MAPPER.readTree(value);
        } catch (JacksonException exception) {
            // The generic messaging error avoids exposing corrupt persisted content.
            throw new ApiException(PlatformMessageKeys.INTERNAL_SERVER_ERROR);
        }
    }
}
