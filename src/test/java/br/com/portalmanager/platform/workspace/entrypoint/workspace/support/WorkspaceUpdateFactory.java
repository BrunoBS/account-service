package br.com.portalmanager.platform.workspace.entrypoint.workspace.support;

import br.com.portalmanager.platform.library.testing.fixture.factory.AbstractTestDataFactory;
import br.com.portalmanager.platform.workspace.entrypoint.workspace.request.ApproverRequest;
import br.com.portalmanager.platform.workspace.entrypoint.workspace.request.UpdateWorkspaceRequest;
import br.com.portalmanager.platform.workspace.entrypoint.workspace.response.WorkspaceResponse;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.stream.IntStream;


public final class WorkspaceUpdateFactory
        extends AbstractTestDataFactory<UpdateWorkspaceRequest, WorkspaceUpdateBuilder> {

    public record InvalidUpdate(String field, UpdateWorkspaceRequest request) {
    }

    @Override
    protected WorkspaceUpdateBuilder builder() {
        return WorkspaceUpdateBuilder.builder();
    }

    public UpdateWorkspaceRequest withoutWorkspaceType(Long version) {
        return builder().withVersion(version).withWorkspaceType(null).build();
    }

    public UpdateWorkspaceRequest withoutName(Long version) {
        return builder().withVersion(version).withName(null).build();
    }

    public UpdateWorkspaceRequest withoutDescription(Long version) {
        return builder().withVersion(version).withDescription(null).build();
    }

    public UpdateWorkspaceRequest withoutRequester(Long version) {
        return builder().withVersion(version).withRequester(null).build();
    }

    public UpdateWorkspaceRequest withoutAcronym(Long version) {
        return builder().withVersion(version).withAcronym(null).build();
    }

    public UpdateWorkspaceRequest withoutEmailGroup(Long version) {
        return builder().withVersion(version).withEmailGroup(null).build();
    }

    public UpdateWorkspaceRequest withoutApprovers(Long version) {
        return builder().withVersion(version).withApprovers(null).build();
    }

    public UpdateWorkspaceRequest unchanged(WorkspaceResponse workspace) {
        return basedOn(workspace).build();
    }

    public UpdateWorkspaceRequest forNonexistentWorkspace() {
        return builder().withVersion(0L).withName("Workspace Inexistente").build();
    }

    public UpdateWorkspaceRequest withVersion(
            WorkspaceResponse workspace,
            Long version) {
        return basedOn(workspace).withVersion(version).build();
    }

    public UpdateWorkspaceRequest renamed(
            WorkspaceResponse workspace,
            String name) {
        return basedOn(workspace).withName(name).build();
    }

    public UpdateWorkspaceRequest changingType(
            WorkspaceResponse workspace,
            String workspaceType) {
        return basedOn(workspace).withWorkspaceType(workspaceType).build();
    }

    public UpdateWorkspaceRequest changingSensitiveFields(
            WorkspaceResponse workspace,
            String workspaceType, String authorizerGroup) {
        return basedOn(workspace).withWorkspaceType(workspaceType)
                .withAuthorizerGroup(authorizerGroup).build();
    }

    private WorkspaceUpdateBuilder basedOn(
            WorkspaceResponse workspace) {
        return builder().withVersion(workspace.version()).withWorkspaceType(workspace.workspaceType())
                .withName(workspace.name()).withDescription(workspace.description())
                .withRequester(workspace.requester()).withAcronym(workspace.acronym())
                .withAuthorizerGroup(workspace.authorizerGroup()).withSettings(workspace.settings())
                .withEmailGroup(workspace.emailGroup())
                .withApprovers(workspace.approvers().stream()
                        .map(approver -> new ApproverRequest(approver.functional(), approver.email())).toList())
                .withTags(workspace.tags());
    }

    public List<InvalidUpdate> missingRequiredFields(Long version) {
        return List.of(
                new InvalidUpdate("workspaceType", withoutWorkspaceType(version)),
                new InvalidUpdate("name", withoutName(version)),
                new InvalidUpdate("description", withoutDescription(version)),
                new InvalidUpdate("requester", withoutRequester(version)),
                new InvalidUpdate("acronym", withoutAcronym(version)),
                new InvalidUpdate("emailGroup", withoutEmailGroup(version)),
                new InvalidUpdate("approvers", withoutApprovers(version)));
    }

    public List<InvalidUpdate> blankRequiredFields(Long version) {
        return List.of(
                new InvalidUpdate("workspaceType", builder().withVersion(version).withWorkspaceType("   ").build()),
                new InvalidUpdate("name", builder().withVersion(version).withName("   ").build()),
                new InvalidUpdate("description", builder().withVersion(version).withDescription("   ").build()),
                new InvalidUpdate("requester", builder().withVersion(version).withRequester("   ").build()),
                new InvalidUpdate("acronym", builder().withVersion(version).withAcronym("   ").build()),
                new InvalidUpdate("emailGroup", builder().withVersion(version).withEmailGroup("   ").build()),
                new InvalidUpdate("approvers[0].functional", builder().withVersion(version)
                        .withApprovers(List.of(new ApproverRequest("   ", "approver@example.com"))).build()),
                new InvalidUpdate("approvers[0].email", builder().withVersion(version)
                        .withApprovers(List.of(new ApproverRequest("F1000", "   "))).build()));
    }

    public List<InvalidUpdate> invalidEmails(Long version) {
        return List.of(
                new InvalidUpdate("emailGroup", builder().withVersion(version).withEmailGroup("invalid-email").build()),
                new InvalidUpdate("approvers[0].email", builder().withVersion(version)
                        .withApprovers(List.of(new ApproverRequest("F1000", "invalid-email"))).build()));
    }

    public UpdateWorkspaceRequest minimumLengths(Long version) {
        return builder().withVersion(version).withWorkspaceType("ADMIN").withName("Min")
                .withDescription("1234567890").withRequester("12345").withAcronym("A")
                .withEmailGroup("a@b.com")
                .withApprovers(List.of(new ApproverRequest("F", "c@d.com"))).build();
    }

    public UpdateWorkspaceRequest withNullOptionalFields(
            WorkspaceResponse workspace) {
        return basedOn(workspace)
                .withSettings(null).withTags(null).build();
    }

    public UpdateWorkspaceRequest withSettings(
            WorkspaceResponse workspace,
            JsonNode settings) {
        return basedOn(workspace).withSettings(settings).build();
    }

    public UpdateWorkspaceRequest withoutApproverItems(
            WorkspaceResponse workspace) {
        return basedOn(workspace).withApprovers(List.of()).build();
    }

    public List<InvalidUpdate> aboveMaximumLengths(
            WorkspaceResponse workspace) {
        return List.of(
                new InvalidUpdate("name", basedOn(workspace).withName("A".repeat(51)).build()),
                new InvalidUpdate("description", basedOn(workspace).withDescription("A".repeat(201)).build()),
                new InvalidUpdate("requester", basedOn(workspace).withRequester("A".repeat(11)).build()),
                new InvalidUpdate("authorizerGroup", basedOn(workspace).withAuthorizerGroup("A".repeat(11)).build()),
                new InvalidUpdate("acronym", basedOn(workspace).withAcronym("A".repeat(6)).build()),
                new InvalidUpdate("tags[0]", basedOn(workspace).withTags(List.of("T".repeat(51))).build()),
                new InvalidUpdate("emailGroup", basedOn(workspace).withEmailGroup("a".repeat(89) + "@example.com").build()),
                new InvalidUpdate("approvers[0].functional", basedOn(workspace).withApprovers(List.of(
                        new ApproverRequest("F".repeat(11), "approver@example.com"))).build()),
                new InvalidUpdate("approvers[0].email", basedOn(workspace).withApprovers(List.of(
                        new ApproverRequest("F1000", "a".repeat(89) + "@example.com"))).build()));
    }

    public List<InvalidUpdate> overCollectionLimits(
            WorkspaceResponse workspace) {
        return List.of(
                new InvalidUpdate("approvers", basedOn(workspace).withApprovers(IntStream.range(0, 6)
                        .mapToObj(index -> new ApproverRequest("F" + index,
                                "user" + index + "@example.com")).toList()).build()),
                new InvalidUpdate("tags", basedOn(workspace).withTags(IntStream.range(0, 21)
                        .mapToObj(index -> "tag-" + index).toList()).build()));
    }

    public UpdateWorkspaceRequest maximumCollections(
            WorkspaceResponse workspace) {
        return basedOn(workspace)
                .withApprovers(IntStream.range(0, 5)
                        .mapToObj(index -> new ApproverRequest("F" + index,
                                "user" + index + "@example.com")).toList())
                .withTags(IntStream.range(0, 20).mapToObj(index -> "tag-" + index).toList()).build();
    }

    public UpdateWorkspaceRequest duplicateApproverFunctionals(
            WorkspaceResponse workspace) {
        return basedOn(workspace)
                .withApprovers(List.of(new ApproverRequest("F1000", "first@example.com"),
                        new ApproverRequest("f1000", "second@example.com"))).build();
    }

    public UpdateWorkspaceRequest duplicateApproverEmails(
            WorkspaceResponse workspace) {
        return basedOn(workspace)
                .withApprovers(List.of(new ApproverRequest("F1000", "same@example.com"),
                        new ApproverRequest("F2000", "SAME@example.com"))).build();
    }

    private UpdateWorkspaceRequest withApprovers(
            WorkspaceResponse workspace,
            List<ApproverRequest> approvers) {
        return basedOn(workspace)
                .withApprovers(approvers).build();
    }

    public UpdateWorkspaceRequest replacingApprovers(
            WorkspaceResponse workspace,
            String description) {
        return basedOn(workspace)
                .withDescription(description)
                .withApprovers(List.of(new ApproverRequest("F2000", "new1@example.com"),
                        new ApproverRequest("F3000", "new2@example.com"))).build();
    }

    public UpdateWorkspaceRequest swappingApproverEmails(
            WorkspaceResponse workspace) {
        return withApprovers(workspace, List.of(
                new ApproverRequest("F1000", "second@example.com"),
                new ApproverRequest("F2000", "first@example.com")));
    }

    public UpdateWorkspaceRequest retainingFirstAndReplacingSecond(
            WorkspaceResponse workspace) {
        var retained = workspace.approvers().getFirst();
        return withApprovers(workspace, List.of(
                new ApproverRequest(retained.functional(), retained.email()),
                new ApproverRequest("F3000", "replacement@example.com")));
    }

    public UpdateWorkspaceRequest withNormalizedTextFields(
            WorkspaceResponse workspace) {
        return basedOn(workspace).withName("  Workspace Update Normalized  ")
                .withDescription("  Description normalized by update  ")
                .withRequester("  updater  ").withAcronym(" UPD ")
                .withAuthorizerGroup("  team_a  ").withEmailGroup("  update@example.com  ")
                .withApprovers(List.of(new ApproverRequest("  F3000  ", "  approver@example.com  ")))
                .build();
    }

    public UpdateWorkspaceRequest withTagsAndMetadata(
            WorkspaceResponse workspace,
            String name,
            String acronym, String authorizerGroup, List<String> tags) {
        return basedOn(workspace).withName(name)
                .withAcronym(acronym).withAuthorizerGroup(authorizerGroup).withTags(tags).build();
    }
}
