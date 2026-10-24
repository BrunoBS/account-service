package br.com.portalmanager.platform.workspace.entrypoint.workspace;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Keeps raw values so malformed controller inputs remain malformed. */
final class WorkspaceRequestBuilder {

    private final Map<String, Object> values = new LinkedHashMap<>();

    private WorkspaceRequestBuilder(String name, String type) {
        values.put("workspaceType", type);
        values.put("name", name);
        values.put("description", "Descrição válida para " + name.trim());
        values.put("requester", "requester");
        values.put("acronym", "WSP");
        values.put("authorizerGroup", null);
        values.put("settings", Map.of("feature", true));
        values.put("emailGroup", "workspace@portalmanager.com");
        values.put("approvers", List.of(Map.of("functional", "F1000", "email", "approver@portalmanager.com")));
        values.put("tags", List.of());
    }

    static WorkspaceRequestBuilder valid(String name, String type) {
        return new WorkspaceRequestBuilder(name, type);
    }

    WorkspaceRequestBuilder with(String field, Object value) {
        values.put(field, value);
        return this;
    }

    Map<String, Object> build() {
        return new LinkedHashMap<>(values);
    }
}
