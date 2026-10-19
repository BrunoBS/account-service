package br.com.portalmanager.platform.workspace.feature.parameter.usecase.operations;


import br.com.portalmanager.platform.library.schemavalidation.normalization.SchemaPayloadNormalizer;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Locale;

@Component
public class ParameterKeyRequestNormalizer implements SchemaPayloadNormalizer {
    @Override
    public JsonNode normalizePayload(JsonNode payload) {
        if (!(payload instanceof ObjectNode object)) return payload;
        ObjectNode normalized = object.deepCopy();
        for (String field : List.of("key", "description", "type", "microserviceIdentifier")) {
            JsonNode value = normalized.get(field);
            if (value != null && value.isString()) {
                String text = value.asString().trim();
                normalized.put(field, field.equals("type") ? text.toUpperCase(Locale.ROOT) : text);
            }
        }
        // ParameterKey JSON and expression operands must remain untouched.
        return normalized;
    }
}
