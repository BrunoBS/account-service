package br.com.portalmanager.platform.workspace.core.application.usecase.operations;

import br.com.portalmanager.platform.library.schemavalidation.normalization.SchemaPayloadNormalizer;
import br.com.portalmanager.platform.library.tagging.model.TagName;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

@Component
public class ApplicationNormalizer implements SchemaPayloadNormalizer {

    @Override
    public JsonNode normalizePayload(JsonNode payload) {
        if (!(payload instanceof ObjectNode object)) return payload;
        ObjectNode normalizedPayload = object.deepCopy();
        for (String field : List.of("name", "alias", "description", "acronym")) {
            JsonNode value = normalizedPayload.get(field);
            if (value != null && value.isString()) normalizedPayload.put(field, trim(value.asString()));
        }
        JsonNode scope = normalizedPayload.get("applicationScope");
        if (scope != null && scope.isString()) normalizedPayload.put("applicationScope", code(scope.asString()));
        JsonNode authorizerGroup = normalizedPayload.get("authorizerGroup");
        if (authorizerGroup != null && authorizerGroup.isString()) {
            String normalizedGroup = group(authorizerGroup.asString());
            if (normalizedGroup == null) normalizedPayload.putNull("authorizerGroup");
            else normalizedPayload.put("authorizerGroup", normalizedGroup);
        }
        return normalizedPayload;
    }

    public String normalizeTag(String value) {
        return value == null || value.isBlank() ? null : TagName.of(value).value();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String code(String value) {
        String normalized = trim(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String group(String value) {
        String normalized = code(value);
        if (normalized == null || normalized.isBlank()) return null;
        return normalized.startsWith("A-") ? normalized : "A-" + normalized;
    }
}
