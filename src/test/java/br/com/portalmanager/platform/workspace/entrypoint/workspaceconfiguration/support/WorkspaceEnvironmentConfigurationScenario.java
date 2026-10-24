package br.com.portalmanager.platform.workspace.entrypoint.workspaceconfiguration.support;

import br.com.portalmanager.platform.library.testing.fixture.scenario.TestScenario;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.CatalogBuilder;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.CatalogSetupClient;
import br.com.portalmanager.platform.workspace.entrypoint.environment.support.EnvironmentFactory;
import br.com.portalmanager.platform.workspace.entrypoint.environment.support.EnvironmentScenario;
import br.com.portalmanager.platform.workspace.entrypoint.environment.workspace.support.WorkspaceEnvironmentClient;
import br.com.portalmanager.platform.workspace.entrypoint.workspace.support.WorkspaceClient;
import br.com.portalmanager.platform.workspace.entrypoint.workspace.support.WorkspaceFactory;
import br.com.portalmanager.platform.workspace.entrypoint.schema.SchemaFactory;
import br.com.portalmanager.platform.workspace.entrypoint.schema.SchemaScenario;
import java.util.Objects;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Prepares the workspace, environment, publisher and schemas used by workspace configuration tests. */
@Component
public final class WorkspaceEnvironmentConfigurationScenario
        implements TestScenario<WorkspaceEnvironmentConfigurationScenario> {
    private final EnvironmentScenario environments;
    private final WorkspaceEnvironmentClient workspaceEnvironments;
    private final WorkspaceClient workspaces;
    private final SchemaScenario schemas;
    private final CatalogSetupClient catalogs;
    private final EnvironmentFactory environmentFactory = new EnvironmentFactory();
    private final SchemaFactory schemaFactory = new SchemaFactory();
    private final WorkspaceFactory workspaceFactory = new WorkspaceFactory();
    private String environmentIdentifier;

    public WorkspaceEnvironmentConfigurationScenario(
            EnvironmentScenario environments,
            WorkspaceEnvironmentClient workspaceEnvironments,
            WorkspaceClient workspaces,
            SchemaScenario schemas,
            CatalogSetupClient catalogs) {
        this.environments = Objects.requireNonNull(environments);
        this.workspaceEnvironments = Objects.requireNonNull(workspaceEnvironments);
        this.workspaces = Objects.requireNonNull(workspaces);
        this.schemas = Objects.requireNonNull(schemas);
        this.catalogs = Objects.requireNonNull(catalogs);
    }

    @Override
    public WorkspaceEnvironmentConfigurationScenario setup() {
        environments.setup();
        schemas.bindResource("PLATFORM", "workspace-environment-configuration",
                schemaFactory.resource("workspace-environment-configuration", "Workspace Environment Configuration",
                        "Contract for workspace configuration, settings and publisher parameters",
                        "Workspace Environment Configuration v1", "workspace-environment-configuration.schema.json"));
        schemas.bindResource("PLATFORM", "workspace-environment-configuration-settings",
                schemaFactory.resource("workspace-environment-configuration-settings", "Workspace Environment Configuration Settings",
                        "Settings contract for workspace environment configurations",
                        "Workspace Environment Configuration Settings v1", "workspace-environment-configuration-settings.json"));
        ensureCatalog("/api/v1/resource-scope-type", "WORKSPACE", "Workspace resource scope", 1);
        ensurePublisher("KAFKA", "Kafka", 1);
        ensurePublisher("WEB_SOCKET", "WebSocket", 2);
        environmentIdentifier = workspaceEnvironments.create(
                        environments.workspaceIdentifier(), environmentFactory.valid())
                .expectCreated().extractBody().identifier();
        return this;
    }

    private void ensureCatalog(String path, String code, String label, int sortOrder) {
        var existing = catalogs.findByCode(path, code);
        if (existing.statusCode() == 404) {
            catalogs.create(path, CatalogBuilder.builder().withCode(code).withLabel(label)
                    .withDescription("Resource scope for integration tests").withSortOrder(sortOrder).build())
                    .expectCreated();
        } else {
            existing.expect2xx();
        }
    }

    private void ensurePublisher(String code, String label, int sortOrder) {
        var existing = catalogs.findByCode("/api/v1/publishers", code);
        if (existing.statusCode() == 404) {
            var settings = JsonMapper.builder().build().createObjectNode().put("scope", "WORKSPACE");
            catalogs.create("/api/v1/publishers", CatalogBuilder.builder().withCode(code)
                    .withLabel(label).withDescription("Publisher for integration tests")
                    .withSortOrder(sortOrder).withSettings(settings).build()).expectCreated();
        } else {
            existing.expect2xx();
        }
    }

    public String workspaceIdentifier() {
        return environments.workspaceIdentifier();
    }

    public String environmentIdentifier() {
        return environmentIdentifier;
    }

    public String createAnotherWorkspace() {
        return workspaces.create(workspaceFactory.manager("Workspace configuration isolation"))
                .expectCreated().extractBody().identifier();
    }
}
