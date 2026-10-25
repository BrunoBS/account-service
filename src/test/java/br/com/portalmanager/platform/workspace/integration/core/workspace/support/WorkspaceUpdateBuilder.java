package br.com.portalmanager.platform.workspace.integration.core.workspace.support;

import br.com.portalmanager.platform.library.testing.fixture.builder.TestDataBuilder;
import br.com.portalmanager.platform.workspace.entrypoint.workspace.request.ApproverRequest;
import br.com.portalmanager.platform.workspace.entrypoint.workspace.request.UpdateWorkspaceRequest;
import net.datafaker.Faker;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

public class WorkspaceUpdateBuilder implements TestDataBuilder<UpdateWorkspaceRequest> {

    private static final Faker faker = new Faker();
    private static final ObjectMapper mapper = new ObjectMapper();

    private Long version = 1L;

    private String workspaceType = "ADMIN";

    private String name = "Workspace " + UUID.randomUUID();

    private String description = faker.lorem().sentence();

    private String requester = faker.regexify("[a-z]{8}");

    private String acronym = faker.regexify("[A-Z]{3}");

    private String authorizerGroup;

    private JsonNode settings = mapper.createObjectNode();

    private String emailGroup = faker.internet().emailAddress();

    private List<ApproverRequest> approvers = List.of(new ApproverRequest(
            "F" + faker.number().digits(4), faker.internet().emailAddress()));

    private List<String> tags = List.of();

    public static WorkspaceUpdateBuilder builder() {
        return new WorkspaceUpdateBuilder();
    }

    public WorkspaceUpdateBuilder withVersion(Long version) {
        this.version = version;
        return this;
    }

    public WorkspaceUpdateBuilder withWorkspaceType(String workspaceType) {
        this.workspaceType = workspaceType;
        return this;
    }

    public WorkspaceUpdateBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public WorkspaceUpdateBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public WorkspaceUpdateBuilder withRequester(String requester) {
        this.requester = requester;
        return this;
    }

    public WorkspaceUpdateBuilder withAcronym(String acronym) {
        this.acronym = acronym;
        return this;
    }

    public WorkspaceUpdateBuilder withAuthorizerGroup(String authorizerGroup) {
        this.authorizerGroup = authorizerGroup;
        return this;
    }

    public WorkspaceUpdateBuilder withSettings(JsonNode settings) {
        this.settings = settings;
        return this;
    }

    public WorkspaceUpdateBuilder withEmailGroup(String emailGroup) {
        this.emailGroup = emailGroup;
        return this;
    }

    public WorkspaceUpdateBuilder withApprovers(List<ApproverRequest> approvers) {
        this.approvers = approvers;
        return this;
    }

    public WorkspaceUpdateBuilder withTags(List<String> tags) {
        this.tags = tags;
        return this;
    }

    @Override
    public UpdateWorkspaceRequest build() {
        return new UpdateWorkspaceRequest(version, workspaceType, name, description, requester, acronym,
                authorizerGroup, settings, emailGroup, approvers, tags);
    }
}
