package br.com.portalmanager.platform.workspace.entrypoint.schema;

import br.com.portalmanager.platform.library.testing.fixture.scenario.TestScenario;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.CatalogScenario;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.SchemaCatalogFactory;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.lifecycle.LifecycleTypeClient;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.lifecycle.LifecycleTypeFactory;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.schema_scope.SchemaScopeTypeClient;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.schema_version_status.SchemaVersionStatusTypeClient;
import br.com.portalmanager.platform.workspace.entrypoint.schema.request.CreateSchemaRequest;
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
        String defaultSchema = createAndPublish(factory.defaultCatalogSchema());
        List.of("DEFAULT", "lifecycle-type", "schema-scope", "schema-version-status-type",
                        "workspace-type", "application-scope-type", "onboarding-phase-type")
                .forEach(code -> bindings.create(factory.binding("CATALOG", code, defaultSchema)).expectCreated());

        CatalogScenario.builder()
                .withLifecycleTypes(lifecycles, lifecycleFactory.all())
                .withSchemaScopes(scopes, catalogFactory.scopes())
                .withSchemaVersionStatuses(versionStatuses, catalogFactory.versionStatuses())
                .setup();

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
