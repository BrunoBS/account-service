package br.com.portalmanager.platform.workspace.integration.core.environment.support;

import br.com.portalmanager.platform.workspace.entrypoint.environment.request.UpdateEnvironmentRequest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class EnvironmentUpdateBuilder {
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private Long version=0L; private String name="Updated environment"; private String description="Updated environment description";
    private String authorizationType="DEV"; private Integer sortOrder; private String authorizerGroup;
    private JsonNode settings=JSON.createObjectNode(); private String environmentTypeCode="CUSTOM"; private String parentIdentifier;
    public static EnvironmentUpdateBuilder builder(){return new EnvironmentUpdateBuilder();}
    public EnvironmentUpdateBuilder withVersion(Long v){version=v;return this;} public EnvironmentUpdateBuilder withName(String v){name=v;return this;}
    public EnvironmentUpdateBuilder withDescription(String v){description=v;return this;} public EnvironmentUpdateBuilder withAuthorizationType(String v){authorizationType=v;return this;}
    public EnvironmentUpdateBuilder withSortOrder(Integer v){sortOrder=v;return this;} public EnvironmentUpdateBuilder withAuthorizerGroup(String v){authorizerGroup=v;return this;}
    public EnvironmentUpdateBuilder withSettings(JsonNode v){settings=v;return this;} public EnvironmentUpdateBuilder withEnvironmentType(String v){environmentTypeCode=v;return this;}
    public EnvironmentUpdateBuilder withParentIdentifier(String v){parentIdentifier=v;return this;}
    public UpdateEnvironmentRequest build(){return new UpdateEnvironmentRequest(version,name,description,authorizationType,sortOrder,authorizerGroup,settings,environmentTypeCode,parentIdentifier);}
}
