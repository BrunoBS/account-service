package br.com.portalmanager.platform.workspace.integration.core.application.support;

import br.com.portalmanager.platform.library.testing.fixture.factory.AbstractTestDataFactory;
import br.com.portalmanager.platform.workspace.entrypoint.application.request.CreateApplicationRequest;
import java.util.List;
import tools.jackson.databind.json.JsonMapper;

public final class ApplicationFactory extends AbstractTestDataFactory<CreateApplicationRequest, ApplicationBuilder> {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    public record InvalidApplication(String field, CreateApplicationRequest request) {}

    @Override
    protected ApplicationBuilder builder() { return ApplicationBuilder.builder(); }

    public CreateApplicationRequest valid() { return builder().build(); }
    public CreateApplicationRequest valid(String name) { return builder().withName(name).build(); }
    public CreateApplicationRequest withName(String name) { return builder().withName(name).build(); }
    public CreateApplicationRequest withAuthorizerGroup(String group) { return builder().withAuthorizerGroup(group).build(); }
    public CreateApplicationRequest withApplicationScope(String scope) { return builder().withApplicationScope(scope).build(); }
    public CreateApplicationRequest withSettings(Object settings) {
        return builder().withName("  Application  ").withSettings(JSON.valueToTree(settings)).build();
    }
    public CreateApplicationRequest withStructuredSettings() {
        return withSettings(java.util.Map.of("language", "JAVA", "infrastructure", "VM"));
    }
    public CreateApplicationRequest withTags(List<String> tags) { return builder().withTags(tags).build(); }
    public CreateApplicationRequest withEmptyTags() { return withTags(List.of()); }

    public CreateApplicationRequest withMaximumLengths() {
        return builder().withName("N".repeat(50)).withAlias("A".repeat(50)).withDescription("D".repeat(200))
                .withAcronym("ABCDE").withAuthorizerGroup("A-" + "G".repeat(8)).withTags(List.of("t".repeat(50))).build();
    }

    public CreateApplicationRequest withInvalidLength(String field) {
        var request = builder();
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

    public List<InvalidApplication> fieldsAboveMaximumLength() {
        return List.of("name", "description", "alias", "acronym", "authorizerGroup", "tags[0]").stream()
                .map(field -> new InvalidApplication(field, withInvalidLength(field))).toList();
    }

    public CreateApplicationRequest withoutAlias() { return builder().withAlias(null).build(); }
    public CreateApplicationRequest withUnsupportedScope() { return withApplicationScope("UNKNOWN"); }
    public CreateApplicationRequest withDuplicateName(String name) { return withName(name); }
    public CreateApplicationRequest withTooManyTags() {
        return withTags(java.util.stream.IntStream.range(0, 21).mapToObj(i -> "tag-" + i).toList());
    }
    public CreateApplicationRequest withDuplicateTags() { return withTags(List.of("managed", "managed")); }
    public CreateApplicationRequest withNullTags() { return builder().withTags(null).build(); }
    public CreateApplicationRequest withNullSettings() { return builder().withSettings(null).build(); }
    public CreateApplicationRequest withInvalidSettings(Object settings) { return withSettings(settings); }
    public CreateApplicationRequest withInvalidSettings() { return withSettings("not-json-object"); }
}
