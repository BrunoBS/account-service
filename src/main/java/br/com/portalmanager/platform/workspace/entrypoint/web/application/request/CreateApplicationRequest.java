package br.com.portalmanager.platform.workspace.entrypoint.web.application.request;

import br.com.portalmanager.platform.workspace.core.application.usecase.model.CreateApplicationInput;
import com.fasterxml.jackson.annotation.JsonSetter;
import tools.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CreateApplicationRequest {
    private String name;
    private String alias;
    private String acronym;
    private String applicationScope;
    private String authorizerGroup;
    private JsonNode settings;
    private List<String> tags;
    private final Map<String, Object> schemaPayload = new LinkedHashMap<>();

    public CreateApplicationRequest() {}

    public String name() { return name; }
    public String alias() { return alias; }
    public String acronym() { return acronym; }
    public String applicationScope() { return applicationScope; }
    public String authorizerGroup() { return authorizerGroup; }
    public JsonNode settings() { return settings; }
    public List<String> tags() { return tags; }

    @JsonSetter("name") public void setName(String value) { name = value; schemaPayload.put("name", value); }
    @JsonSetter("alias") public void setAlias(String value) { alias = value; schemaPayload.put("alias", value); }
    @JsonSetter("acronym") public void setAcronym(String value) { acronym = value; schemaPayload.put("acronym", value); }
    @JsonSetter("applicationScope") public void setApplicationScope(String value) { applicationScope = value; schemaPayload.put("applicationScope", value); }
    @JsonSetter("authorizerGroup") public void setAuthorizerGroup(String value) { authorizerGroup = value; schemaPayload.put("authorizerGroup", value); }
    @JsonSetter("settings") public void setSettings(JsonNode value) { settings = value; schemaPayload.put("settings", value); }
    @JsonSetter("tags") public void setTags(List<String> value) { tags = value; schemaPayload.put("tags", value); }

    public CreateApplicationInput toInput() {
        return new CreateApplicationInput(name, alias, acronym, applicationScope, authorizerGroup, settings, tags);
    }

    public Map<String, Object> schemaPayload() {
        return Map.copyOf(schemaPayload);
    }
}
