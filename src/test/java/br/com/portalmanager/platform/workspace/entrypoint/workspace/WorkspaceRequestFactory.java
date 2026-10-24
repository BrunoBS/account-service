package br.com.portalmanager.platform.workspace.entrypoint.workspace;

import java.util.Map;

final class WorkspaceRequestFactory {

    private WorkspaceRequestFactory() {
    }

    static Map<String, Object> validCreate(String name, String type) {
        return WorkspaceRequestBuilder.valid(name, type).build();
    }

    static Map<String, Object> validUpdate(Integer version, String name, String type) {
        return WorkspaceRequestBuilder.valid(name, type).with("version", version).build();
    }

    static Map<String, Object> invalidField(String name, String type, String field, Object value) {
        return WorkspaceRequestBuilder.valid(name, type).with(field, value).build();
    }
}
