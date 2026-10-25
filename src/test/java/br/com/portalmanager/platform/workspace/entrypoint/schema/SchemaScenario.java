package br.com.portalmanager.platform.workspace.entrypoint.schema;

import br.com.portalmanager.platform.library.testing.fixture.scenario.TestScenario;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.CatalogScenario;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.SchemaCatalogFactory;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.lifecycle.LifecycleTypeClient;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.lifecycle.LifecycleTypeFactory;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.schema_scope.SchemaScopeTypeClient;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.schema_version_status.SchemaVersionStatusTypeClient;
import br.com.portalmanager.platform.workspace.entrypoint.schema.request.CreateSchemaRequest;
import br.com.portalmanager.platform.workspace.entrypoint.schema.response.SchemaBindingResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public final class SchemaScenario implements TestScenario<SchemaScenario> {
    private final SchemaClient schemas;
    private final SchemaBindingClient bindings;
    private final LifecycleTypeClient lifecycles;
    private final SchemaScopeTypeClient scopes;
    private final SchemaVersionStatusTypeClient versionStatuses;
    private final SchemaFactory factory = new SchemaFactory();
    private final LifecycleTypeFactory lifecycleFactory = new LifecycleTypeFactory();
    private final SchemaCatalogFactory catalogFactory = new SchemaCatalogFactory();

    public SchemaScenario(
            SchemaClient schemas,
            SchemaBindingClient bindings,
            LifecycleTypeClient lifecycles,
            SchemaScopeTypeClient scopes,
            SchemaVersionStatusTypeClient versionStatuses) {
        this.schemas = Objects.requireNonNull(schemas);
        this.bindings = Objects.requireNonNull(bindings);
        this.lifecycles = Objects.requireNonNull(lifecycles);
        this.scopes = Objects.requireNonNull(scopes);
        this.versionStatuses = Objects.requireNonNull(versionStatuses);
    }

    @Override
    public SchemaScenario setup() {
        CatalogScenario.builder()
                .withLifecycleTypes(lifecycles, lifecycleFactory.all())
                .withSchemaScopes(scopes, catalogFactory.scopes())
                .withSchemaVersionStatuses(versionStatuses, catalogFactory.versionStatuses())
                .setup();

        String defaultSchema = schemas.findAll().expectOk().extractSchemas().stream()
                .filter(schema -> "default".equals(schema.code()) && "PLATFORM".equals(schema.scope()))
                .map(schema -> schema.identifier())
                .findFirst()
                .orElseGet(() -> createAndPublish(factory.defaultCatalogSchema()));

        List<SchemaBindingResponse> existingBindings = bindings.findAll().expectOk().extractBindings();
        List.of("WORKSPACE", "APPLICATION", "ENVIRONMENT", "PUBLISHER", "FEATURE", "MICROSERVICE",
                        "CATALOG", "SERVICE_CONTEXT", "MESSAGE", "SEGMENTATION", "PLATFORM")
                .stream()
                .filter(code -> existingBindings.stream().noneMatch(binding ->
                        code.equals(binding.resourceType()) && "DEFAULT".equals(binding.resourceCode())))
                .forEach(resourceType -> bindings.create(
                        factory.binding(resourceType, "DEFAULT", defaultSchema)).expectCreated());

        List.of("lifecycle-type", "schema-scope", "schema-version-status-type", "workspace-type",
                        "application-scope-type", "onboarding-phase-type")
                .stream()
                .filter(code -> existingBindings.stream().noneMatch(binding ->
                        "CATALOG".equals(binding.resourceType()) && code.equals(binding.resourceCode())))
                .forEach(code -> bindings.create(factory.binding("CATALOG", code, defaultSchema)).expectCreated());

        String workspaceSchema = createAndPublish(factory.workspaceResourceSchema());
        bindings.create(factory.binding("WORKSPACE", "workspace", workspaceSchema)).expectCreated();
        return this;
    }

    public SchemaScenario bindResource(String resourceType, String resourceCode, CreateSchemaRequest request) {
        String schemaIdentifier = createAndPublish(request);
        bindings.create(factory.binding(resourceType, resourceCode, schemaIdentifier)).expectCreated();
        return this;
    }

    public SchemaScenario bindResources(String resourceType, List<String> resourceCodes, CreateSchemaRequest request) {
        String schemaIdentifier = createAndPublish(request);
        resourceCodes.forEach(code -> bindings.create(factory.binding(resourceType, code, schemaIdentifier)).expectCreated());
        return this;
    }

    private String createAndPublish(CreateSchemaRequest request) {
        var schema = schemas.create(request).expectCreated().extractSchema();
        String versionIdentifier = schemas.findVersions(schema.identifier()).expectOk().extractVersionIdentifier();
        schemas.publish(schema.identifier(), versionIdentifier).expectOk();
        return schema.identifier();
    }
}
