package br.com.portalmanager.platform.workspace.core.workspace.usecase.operations;

import br.com.portalmanager.platform.library.schemavalidation.normalization.SchemaPayloadNormalizer;
import br.com.portalmanager.platform.library.tagging.model.TagName;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

@Component
public class WorkspaceNormalizer implements SchemaPayloadNormalizer {

    private static final List<String> TEXT_FIELDS = List.of(
        "name",
        "description",
        "requester",
        "acronym",
        "emailGroup"
    );

    @Override
    public JsonNode normalizePayload(JsonNode payload) {
        if (!(payload instanceof ObjectNode payloadObject)) return payload;
        ObjectNode normalizedPayload = payloadObject.deepCopy();
        TEXT_FIELDS.forEach(field -> trimText(normalizedPayload, field));
        normalizeCode(normalizedPayload, "workspaceType");
        normalizeCode(normalizedPayload, "authorizerGroup");
        JsonNode approvers = normalizedPayload.get("approvers");
        if (approvers != null && approvers.isArray()) {
            for (JsonNode approver : approvers) {
                if (approver instanceof ObjectNode approverObject) {
                    trimText(approverObject, "functional");
                    trimText(approverObject, "email");
                }
            }
        }
        return normalizedPayload;
    }

    public String normalizeTypeFilter(String value) {
        return normalizeCode(value);
    }

    public String normalizeTagFilter(String value) {
        return value == null || value.isBlank() ? null : TagName.of(value).value();
    }

    private void trimText(ObjectNode object, String field) {
        JsonNode value = object.get(field);
        if (value != null && value.isString()) object.put(field, value.asString().trim());
    }

    private void normalizeCode(ObjectNode object, String field) {
        JsonNode value = object.get(field);
        if (value == null || !value.isString()) return;
        String normalized = normalizeCode(value.asString());
        if (normalized == null) object.putNull(field);
        else object.put(field, normalized);
    }

    private String normalizeCode(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
