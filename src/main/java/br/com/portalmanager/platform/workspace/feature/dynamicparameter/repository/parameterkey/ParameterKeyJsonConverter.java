package br.com.portalmanager.platform.workspace.feature.dynamicparameter.repository.parameterkey;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.messaging.model.PlatformErrorDefinition;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Converter(autoApply = true)
public class ParameterKeyJsonConverter implements AttributeConverter<JsonNode, String> {
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final PlatformErrorDefinition INVALID_JSON =
            new PlatformErrorDefinition(
                    "PARAMETER-KEY-0501",
                    "Invalid persisted parameter key JSON.",
                    "Check parameter key storage integrity.",
                    500);

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
            // Do not attach a parser exception containing the persisted value.
            throw new PlatformConfigurationException(INVALID_JSON);
        }
    }
}
