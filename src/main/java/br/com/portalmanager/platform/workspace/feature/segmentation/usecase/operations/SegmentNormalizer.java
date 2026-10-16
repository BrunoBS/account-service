package br.com.portalmanager.platform.workspace.feature.segmentation.usecase.operations;

import br.com.portalmanager.platform.library.schemavalidation.normalization.SchemaPayloadNormalizer;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Locale;

@Component
public class SegmentNormalizer implements SchemaPayloadNormalizer {
    @Override
    public JsonNode normalizePayload(JsonNode payload) {
        if (!(payload instanceof ObjectNode object)) return payload;
        ObjectNode normalized = object.deepCopy();
        for (String field : List.of("code", "name")) {
            JsonNode value = normalized.get(field);
            if (value != null && value.isString()) {
                String text = value.asString().trim();
                normalized.put(field, field.equals("name") ? text : text.toLowerCase(Locale.ROOT));
            }
        }
        return normalized;
    }
}
