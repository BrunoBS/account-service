package br.com.portalmanager.platform.workspace.entrypoint.application;

import br.com.portalmanager.platform.library.testing.fixture.factory.AbstractTestDataFactory;
import br.com.portalmanager.platform.workspace.entrypoint.application.request.UpdateApplicationRequest;
import java.util.List;
import tools.jackson.databind.json.JsonMapper;

public final class ApplicationUpdateFactory extends AbstractTestDataFactory<UpdateApplicationRequest, ApplicationUpdateBuilder> {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    public record InvalidUpdate(String field, UpdateApplicationRequest request) {}

    @Override
    protected ApplicationUpdateBuilder builder() { return ApplicationUpdateBuilder.builder(); }

    public UpdateApplicationRequest valid(Long version) { return builder().withVersion(version).build(); }
    public UpdateApplicationRequest renamed(Long version, String name) {
        return builder().withVersion(version).withName(name).build();
    }
    public UpdateApplicationRequest renamed(Long version, String name, String group, List<String> tags) {
        return builder().withVersion(version).withName(name).withAuthorizerGroup(group).withTags(tags).build();
    }
    public UpdateApplicationRequest renamedWithManualTag(Long version, String name, String group, String tag) {
        return builder().withVersion(version).withName(name).withAuthorizerGroup(group).withTags(List.of(tag)).build();
    }
    public UpdateApplicationRequest withSettings(Long version, Object settings) {
        return builder().withVersion(version).withSettings(JSON.valueToTree(settings)).build();
    }
    public UpdateApplicationRequest withScope(Long version, String name, String scope) {
        return builder().withVersion(version).withName(name).withApplicationScope(scope).build();
    }
    public UpdateApplicationRequest withInvalidLength(Long version, String field) {
        var request = builder().withVersion(version);
        return switch (field) {
            case "name" -> request.withName("N".repeat(51)).build();
            case "description" -> request.withDescription("D".repeat(201)).build();
            case "alias" -> request.withAlias("A".repeat(51)).build();
            case "acronym" -> request.withAcronym("ABCDEF").build();
            case "authorizerGroup" -> request.withAuthorizerGroup("A-" + "G".repeat(9)).build();
            case "tags[0]" -> request.withTags(List.of("t".repeat(51))).build();
            default -> throw new IllegalArgumentException("Unsupported application field: " + field);
        };
    }
    public UpdateApplicationRequest withInvalidSettings(Long version) {
        return withSettings(version, "not-json-object");
    }
    public List<InvalidUpdate> fieldsAboveMaximumLength(Long version) {
        return List.of("name", "description", "alias", "acronym", "authorizerGroup", "tags[0]").stream()
                .map(field -> new InvalidUpdate(field, withInvalidLength(version, field))).toList();
    }
}
