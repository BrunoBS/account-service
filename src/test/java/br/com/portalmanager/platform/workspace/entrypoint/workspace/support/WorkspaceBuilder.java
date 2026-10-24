package br.com.portalmanager.platform.workspace.entrypoint.workspace.support;

import br.com.portalmanager.platform.library.testing.fixture.builder.TestDataBuilder;
import br.com.portalmanager.platform.workspace.entrypoint.workspace.request.ApproverRequest;
import br.com.portalmanager.platform.workspace.entrypoint.workspace.request.CreateWorkspaceRequest;
import br.com.portalmanager.platform.workspace.entrypoint.workspace.request.UpdateWorkspaceRequest;
import net.datafaker.Faker;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

public class WorkspaceBuilder implements TestDataBuilder<CreateWorkspaceRequest> {

    private static final Faker faker = new Faker();
    private static final ObjectMapper mapper = new ObjectMapper();

    private Long version;

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

    public static WorkspaceBuilder builder() {
        return new WorkspaceBuilder();
    }

    public WorkspaceBuilder withVersion(Long version) {
        this.version = version;
        return this;
    }

    public WorkspaceBuilder withWorkspaceType(String workspaceType) {
        this.workspaceType = workspaceType;
        return this;
    }

    public WorkspaceBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public WorkspaceBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public WorkspaceBuilder withRequester(String requester) {
        this.requester = requester;
        return this;
    }

    public WorkspaceBuilder withAcronym(String acronym) {
        this.acronym = acronym;
        return this;
    }

    public WorkspaceBuilder withAuthorizerGroup(String authorizerGroup) {
        this.authorizerGroup = authorizerGroup;
        return this;
    }

    public WorkspaceBuilder withSettings(JsonNode settings) {
        this.settings = settings;
        return this;
    }

    public WorkspaceBuilder withEmailGroup(String emailGroup) {
        this.emailGroup = emailGroup;
        return this;
    }

    public WorkspaceBuilder withApprovers(List<ApproverRequest> approvers) {
        this.approvers = approvers;
        return this;
    }

    public WorkspaceBuilder withTags(List<String> tags) {
        this.tags = tags;
        return this;
    }

    @Override
    public CreateWorkspaceRequest build() {
        return new CreateWorkspaceRequest(version, workspaceType, name, description, requester, acronym,
                authorizerGroup, settings, emailGroup, approvers, tags);
    }

    /** Update has a distinct production request type but the same payload fields. */
    public UpdateWorkspaceRequest buildUpdate() {
        return new UpdateWorkspaceRequest(version, workspaceType, name, description, requester, acronym,
                authorizerGroup, settings, emailGroup, approvers, tags);
    }
}
