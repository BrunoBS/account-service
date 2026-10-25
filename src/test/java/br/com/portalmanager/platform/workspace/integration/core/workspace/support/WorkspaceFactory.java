package br.com.portalmanager.platform.workspace.integration.core.workspace.support;

import br.com.portalmanager.platform.library.testing.fixture.factory.AbstractTestDataFactory;
import br.com.portalmanager.platform.workspace.entrypoint.workspace.request.ApproverRequest;
import br.com.portalmanager.platform.workspace.entrypoint.workspace.request.CreateWorkspaceRequest;

import java.util.List;
import java.util.stream.IntStream;
import tools.jackson.databind.JsonNode;

public final class WorkspaceFactory extends AbstractTestDataFactory<CreateWorkspaceRequest, WorkspaceBuilder> {

    public record InvalidWorkspace(String field, CreateWorkspaceRequest request) {}

    @Override
    protected WorkspaceBuilder builder() {
        return WorkspaceBuilder.builder();
    }

    public CreateWorkspaceRequest withoutName() {
        return builder().withName(null).build();
    }

    public CreateWorkspaceRequest withoutDescription() {
        return builder().withDescription(null).build();
    }

    public CreateWorkspaceRequest withoutRequester() {
        return builder().withRequester(null).build();
    }

    public CreateWorkspaceRequest withoutAcronym() {
        return builder().withAcronym(null).build();
    }

    public CreateWorkspaceRequest withoutAuthorizerGroup() {
        return builder().withAuthorizerGroup(null).build();
    }

    public CreateWorkspaceRequest withoutEmailGroup() {
        return builder().withEmailGroup(null).build();
    }

    public CreateWorkspaceRequest withoutWorkspaceType() {
        return builder().withWorkspaceType(null).build();
    }

    public CreateWorkspaceRequest withoutApprovers() {
        return builder().withApprovers(null).build();
    }

    public CreateWorkspaceRequest withoutApproverFunctional() {
        return builder().withApprovers(List.of(new ApproverRequest(null, "approver@example.com"))).build();
    }

    public CreateWorkspaceRequest withoutApproverEmail() {
        return builder().withApprovers(List.of(new ApproverRequest("F1000", null))).build();
    }

    public CreateWorkspaceRequest withInvalidApproverEmail() {
        return builder().withApprovers(List.of(new ApproverRequest("F1000", "not-an-email"))).build();
    }

    public CreateWorkspaceRequest withInvalidRequiredFields() {
        return builder().withWorkspaceType("INVALID")
                .withName("X")
                .withDescription("short")
                .withRequester("x")
                .withAcronym("ACRONYM_TOO_LONG")
                .withEmailGroup("invalid-email")
                .withApprovers(List.of())
                .build();
    }

    public CreateWorkspaceRequest withUnsupportedWorkspaceType() {
        return builder().withName("Workspace Catalog Type").withWorkspaceType("CATALOG").build();
    }

    public CreateWorkspaceRequest withVersionOnCreate() {
        return builder().withVersion(1L).build();
    }

    public CreateWorkspaceRequest withDuplicateApproverFunctionals() {
        return builder().withName("Workspace Duplicate Functional")
                .withApprovers(List.of(new ApproverRequest("F1000", "first@example.com"),
                        new ApproverRequest("f1000", "second@example.com")))
                .build();
    }

    public CreateWorkspaceRequest withDuplicateApproverEmails() {
        return builder().withName("Workspace Duplicate Email")
                .withApprovers(List.of(new ApproverRequest("F1000", "same@example.com"),
                        new ApproverRequest("F2000", "SAME@example.com")))
                .build();
    }

    public CreateWorkspaceRequest withoutTags() {
        return builder().withTags(null).build();
    }

    public CreateWorkspaceRequest withBlankWorkspaceType() {
        return builder().withWorkspaceType("   ").build();
    }

    public CreateWorkspaceRequest withBlankName() {
        return builder().withName("   ").build();
    }

    public CreateWorkspaceRequest withBlankDescription() {
        return builder().withDescription("   ").build();
    }

    public CreateWorkspaceRequest withBlankRequester() {
        return builder().withRequester("   ").build();
    }

    public CreateWorkspaceRequest withBlankAcronym() {
        return builder().withAcronym("   ").build();
    }

    public CreateWorkspaceRequest withBlankEmailGroup() {
        return builder().withEmailGroup("   ").build();
    }

    public CreateWorkspaceRequest withBlankApproverFunctional() {
        return builder().withApprovers(List.of(new ApproverRequest("   ", "approver@example.com"))).build();
    }

    public CreateWorkspaceRequest withBlankApproverEmail() {
        return builder().withApprovers(List.of(new ApproverRequest("F1000", "   "))).build();
    }

    public List<InvalidWorkspace> blankRequiredFields() {
        return List.of(
                new InvalidWorkspace("workspaceType", withBlankWorkspaceType()),
                new InvalidWorkspace("name", withBlankName()),
                new InvalidWorkspace("description", withBlankDescription()),
                new InvalidWorkspace("requester", withBlankRequester()),
                new InvalidWorkspace("acronym", withBlankAcronym()),
                new InvalidWorkspace("emailGroup", withBlankEmailGroup()),
                new InvalidWorkspace("approvers[0].functional", withBlankApproverFunctional()),
                new InvalidWorkspace("approvers[0].email", withBlankApproverEmail()));
    }

    public CreateWorkspaceRequest withNameAboveMaximumLength() {
        return builder().withName("A".repeat(51)).build();
    }

    public CreateWorkspaceRequest withDescriptionAboveMaximumLength() {
        return builder().withDescription("A".repeat(201)).build();
    }

    public CreateWorkspaceRequest withAcronymAboveMaximumLength() {
        return builder().withAcronym("A".repeat(6)).build();
    }

    public CreateWorkspaceRequest withTagAboveMaximumLength() {
        return builder().withTags(List.of("T".repeat(51))).build();
    }

    public CreateWorkspaceRequest withRequesterAboveMaximumLength() {
        return builder().withRequester("A".repeat(11)).build();
    }

    public CreateWorkspaceRequest withAuthorizerGroupAboveMaximumLength() {
        return builder().withAuthorizerGroup("A".repeat(11)).build();
    }

    public CreateWorkspaceRequest withEmailGroupAboveMaximumLength() {
        return builder().withEmailGroup("a".repeat(89) + "@example.com").build();
    }

    public CreateWorkspaceRequest withApproverFunctionalAboveMaximumLength() {
        return builder().withApprovers(List.of(new ApproverRequest("F".repeat(11), "approver@example.com")))
                .build();
    }

    public CreateWorkspaceRequest withApproverEmailAboveMaximumLength() {
        return builder().withApprovers(List.of(new ApproverRequest("F1000", "a".repeat(89) + "@example.com")))
                .build();
    }

    public List<InvalidWorkspace> fieldsAboveMaximumLength() {
        return List.of(
                new InvalidWorkspace("name", withNameAboveMaximumLength()),
                new InvalidWorkspace("description", withDescriptionAboveMaximumLength()),
                new InvalidWorkspace("acronym", withAcronymAboveMaximumLength()),
                new InvalidWorkspace("tags[0]", withTagAboveMaximumLength()),
                new InvalidWorkspace("requester", withRequesterAboveMaximumLength()),
                new InvalidWorkspace("authorizerGroup", withAuthorizerGroupAboveMaximumLength()),
                new InvalidWorkspace("emailGroup", withEmailGroupAboveMaximumLength()),
                new InvalidWorkspace("approvers[0].functional", withApproverFunctionalAboveMaximumLength()),
                new InvalidWorkspace("approvers[0].email", withApproverEmailAboveMaximumLength()));
    }

    public CreateWorkspaceRequest withSixApprovers() {
        return builder().withApprovers(java.util.stream.IntStream.range(0, 6)
                .mapToObj(index -> new ApproverRequest("F" + index, "user" + index + "@example.com"))
                .toList()).build();
    }

    public CreateWorkspaceRequest withTwentyOneTags() {
        return builder().withTags(IntStream.range(0, 21)
                .mapToObj(index -> "tag-" + index)
                .toList()).build();
    }

    public CreateWorkspaceRequest minimumLengths() {
        return builder().withWorkspaceType("ADMIN")
                .withName("Min")
                .withDescription("1234567890")
                .withRequester("12345")
                .withAcronym("A")
                .withEmailGroup("a@b.com")
                .withApprovers(List.of(new ApproverRequest("F", "c@d.com")))
                .build();
    }

    public CreateWorkspaceRequest maximumCollectionSizes() {
        var approvers = IntStream.range(0, 5)
                .mapToObj(index -> new ApproverRequest("F" + index, "user" + index + "@example.com"))
                .toList();
        var tags = IntStream.range(0, 20).mapToObj(index -> "tag-" + index).toList();
        return builder().withApprovers(approvers).withTags(tags).build();
    }

    public CreateWorkspaceRequest withNullSettingsAndTags() {
        return builder().withSettings(null).withTags(null).build();
    }

    public CreateWorkspaceRequest withSettings(JsonNode settings) {
        return builder().withSettings(settings).build();
    }

    public CreateWorkspaceRequest withWorkspaceForApproverReconciliation() {
        return builder().withName("Workspace Approver Reconciliation")
                .withApprovers(List.of(new ApproverRequest("F1000", "first@example.com"),
                        new ApproverRequest("F2000", "second@example.com"))).build();
    }

    public CreateWorkspaceRequest admin(String name) {
        return builder().withWorkspaceType("ADMIN").withName(name).build();
    }

    public CreateWorkspaceRequest adminWithAuthorizerGroup(String name, String authorizerGroup) {
        return builder().withWorkspaceType("ADMIN").withName(name)
                .withAuthorizerGroup(authorizerGroup).build();
    }

    public CreateWorkspaceRequest manager(String name) {
        return builder().withWorkspaceType("MANAGER").withName(name).build();
    }

    public CreateWorkspaceRequest managerWithAuthorizerGroup(String name, String authorizerGroup) {
        return builder().withWorkspaceType("MANAGER").withName(name)
                .withAuthorizerGroup(authorizerGroup).build();
    }

    public CreateWorkspaceRequest managerWithBlankAuthorizerGroup(String name) {
        return builder().withWorkspaceType("MANAGER").withName(name)
                .withAuthorizerGroup("   ").build();
    }

    public CreateWorkspaceRequest withNormalizedFields() {
        return builder().withWorkspaceType(" admin ")
                .withName("  Workspace G3  ")
                .withDescription("  Descrição válida do workspace G3  ")
                .withRequester("  requester  ")
                .withAcronym(" G3 ")
                .withAuthorizerGroup("  GRP_WORK  ")
                .withEmailGroup("  workspace@portalmanager.com  ")
                .withApprovers(List.of(new ApproverRequest("  F1234  ", "  approver@portalmanager.com  ")))
                .build();
    }

    public CreateWorkspaceRequest withManualTagsForNormalization() {
        return builder().withWorkspaceType("ADMIN").withName("Workspace Tags")
                .withAcronym("TAG").withAuthorizerGroup("TEAM_A")
                .withTags(List.of("  Minha   Tag  ", "minha\tTag", "OUTRA TAG", "   "))
                .build();
    }

    public CreateWorkspaceRequest withManualTags(String name, String acronym, String authorizerGroup,
            List<String> tags) {
        return builder().withWorkspaceType("ADMIN").withName(name).withAcronym(acronym)
                .withAuthorizerGroup(authorizerGroup).withTags(tags).build();
    }

    public CreateWorkspaceRequest withConcurrentName(String name) {
        return builder().withWorkspaceType("ADMIN").withName(name).build();
    }

    public CreateWorkspaceRequest withNameContainingSpaces(String name) {
        return builder().withWorkspaceType("ADMIN").withName(name).build();
    }

    public CreateWorkspaceRequest withApproverDuplicatesByFunctional() {
        return builder().withApprovers(List.of(new ApproverRequest("F1000", "first@example.com"),
                new ApproverRequest("f1000", "second@example.com"))).build();
    }

    public CreateWorkspaceRequest withApproverDuplicatesByEmail() {
        return builder().withApprovers(List.of(new ApproverRequest("F1000", "same@example.com"),
                new ApproverRequest("F2000", "SAME@example.com"))).build();
    }

    public CreateWorkspaceRequest duplicatedName(String existingName) {
        return builder().withName(existingName).build();
    }

    public CreateWorkspaceRequest custom(String name, String workspaceType) {
        return builder().withName(name).withWorkspaceType(workspaceType).build();
    }

    public CreateWorkspaceRequest maximumLengths() {
        String maximumEmail = "a".repeat(63) + "@" + "b".repeat(32) + ".com";
        return builder().withWorkspaceType("ADMIN")
                .withName("N".repeat(50))
                .withDescription("D".repeat(200))
                .withRequester("R".repeat(10))
                .withAcronym("ABCDE")
                .withAuthorizerGroup("G".repeat(10))
                .withEmailGroup(maximumEmail)
                .withTags(List.of("T".repeat(50)))
                .withApprovers(List.of(new ApproverRequest("F".repeat(10), maximumEmail)))
                .build();
    }
}
