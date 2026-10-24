package br.com.portalmanager.platform.workspace.entrypoint.application;

import br.com.portalmanager.platform.library.testing.fixture.builder.TestDataBuilder;
import br.com.portalmanager.platform.workspace.entrypoint.application.request.CreateApplicationRequest;
import java.util.List;
import java.util.UUID;
import net.datafaker.Faker;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class ApplicationBuilder implements TestDataBuilder<CreateApplicationRequest> {
    private static final Faker FAKER = new Faker();
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private Long version;
    private String name = "Application " + UUID.randomUUID();
    private String alias = "application-" + FAKER.regexify("[a-z]{6}");
    private String description = FAKER.lorem().sentence();
    private String acronym = "APP";
    private String applicationScope = "BACKEND";
    private String authorizerGroup;
    private JsonNode settings = JSON.createObjectNode().put("language", "JAVA").put("infrastructure", "VM");
    private List<String> tags = List.of("managed");

    public static ApplicationBuilder builder() { return new ApplicationBuilder(); }
    public ApplicationBuilder withVersion(Long value) { version = value; return this; }
    public ApplicationBuilder withName(String value) { name = value; return this; }
    public ApplicationBuilder withAlias(String value) { alias = value; return this; }
    public ApplicationBuilder withDescription(String value) { description = value; return this; }
    public ApplicationBuilder withAcronym(String value) { acronym = value; return this; }
    public ApplicationBuilder withApplicationScope(String value) { applicationScope = value; return this; }
    public ApplicationBuilder withAuthorizerGroup(String value) { authorizerGroup = value; return this; }
    public ApplicationBuilder withSettings(JsonNode value) { settings = value; return this; }
    public ApplicationBuilder withTags(List<String> value) { tags = value; return this; }

    @Override
    public CreateApplicationRequest build() {
        return new CreateApplicationRequest(version, name, alias, description, acronym,
                applicationScope, authorizerGroup, settings, tags);
    }

}
