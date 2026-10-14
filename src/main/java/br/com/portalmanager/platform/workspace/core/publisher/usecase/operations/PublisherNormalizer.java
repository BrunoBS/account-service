package br.com.portalmanager.platform.workspace.core.publisher.usecase.operations;

import br.com.portalmanager.platform.library.schemavalidation.normalization.SchemaPayloadNormalizer;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

@Component
public class PublisherNormalizer implements SchemaPayloadNormalizer {

    @Override
    public JsonNode normalizePayload(JsonNode payload) {
        if (!(payload instanceof ObjectNode object)) return payload;
        ObjectNode normalized = object.deepCopy();
        for (String field : List.of("name", "description", "code", "scope")) {
            JsonNode value = normalized.get(field);
            if (value != null && value.isString()) {
                String text = value.asString().trim();
                normalized.put(
                    field,
                    field.equals("code") || field.equals("scope") ? text.toUpperCase(Locale.ROOT) : text
                );
            }
        }
        return normalized;
    }

    public String normalizeScope(String scope) {
        return scope == null ? null : scope.trim().toUpperCase(Locale.ROOT);
    }
}
