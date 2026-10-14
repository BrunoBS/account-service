package br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment;

import br.com.portalmanager.platform.library.schemavalidation.normalization.SchemaPayloadNormalizer;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.CreateEnvironmentInput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.UpdateEnvironmentInput;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

@Component
public class EnvironmentNormalizer implements SchemaPayloadNormalizer {

    private static final List<String> TEXT_FIELDS = List.of("name", "description");

    @Override
    public JsonNode normalizePayload(JsonNode payload) {
        if (!(payload instanceof ObjectNode payloadObject)) return payload;
        ObjectNode normalizedPayload = payloadObject.deepCopy();
        TEXT_FIELDS.forEach(field -> trimText(normalizedPayload, field));
        trimText(normalizedPayload, "parentIdentifier");
        trimText(normalizedPayload, "parentTypeCode");
        trimText(normalizedPayload, "childTypeCode");
        normalizeCode(normalizedPayload, "authorizationType");
        trimText(normalizedPayload, "environmentTypeCode");
        normalizeCode(normalizedPayload, "authorizerGroup");
        normalizeCode(normalizedPayload, "code");
        return normalizedPayload;
    }

    public CreateEnvironmentInput normalize(CreateEnvironmentInput input) {
        if (input == null) return null;
        return new CreateEnvironmentInput(
            input.version(),
            trim(input.name()),
            trim(input.description()),
            authorization(input.authorizationType()),
            input.sortOrder(),
            group(input.authorizerGroup()),
            settings(input.settings()),
            trim(input.environmentTypeCode()),
            trim(input.parentIdentifier())
        );
    }

    public UpdateEnvironmentInput normalize(UpdateEnvironmentInput input) {
        if (input == null) return null;
        return new UpdateEnvironmentInput(
            input.version(),
            trim(input.name()),
            trim(input.description()),
            authorization(input.authorizationType()),
            input.sortOrder(),
            group(input.authorizerGroup()),
            settings(input.settings()),
            trim(input.environmentTypeCode()),
            trim(input.parentIdentifier())
        );
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String authorization(String value) {
        return code(value);
    }

    private String group(String value) {
        String group = code(value);
        if (group == null) return null;
        return group.startsWith("E-") ? group : "E-" + group;
    }

    private JsonNode settings(JsonNode value) {
        if (value == null || value.isNull()) return tools.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        return value.deepCopy();
    }

    private String code(String value) {
        String normalized = trim(value);
        return normalized == null || normalized.isBlank() ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private void trimText(ObjectNode object, String field) {
        JsonNode value = object.get(field);
        if (value != null && value.isString()) object.put(field, value.asString().trim());
    }

    private void normalizeCode(ObjectNode object, String field) {
        JsonNode value = object.get(field);
        if (value == null || !value.isString()) return;
        String normalizedCode = code(value.asString());
        if (normalizedCode == null) object.putNull(field);
        else object.put(field, normalizedCode);
    }
}
