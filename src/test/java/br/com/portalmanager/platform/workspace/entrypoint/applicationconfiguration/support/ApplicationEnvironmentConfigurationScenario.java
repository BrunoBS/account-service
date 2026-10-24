package br.com.portalmanager.platform.workspace.entrypoint.applicationconfiguration.support;

import br.com.portalmanager.platform.library.testing.fixture.scenario.TestScenario;
import br.com.portalmanager.platform.workspace.entrypoint.application.ApplicationClient;
import br.com.portalmanager.platform.workspace.entrypoint.application.ApplicationFactory;
import br.com.portalmanager.platform.workspace.entrypoint.application.ApplicationScenario;
import br.com.portalmanager.platform.workspace.entrypoint.workspaceconfiguration.support.WorkspaceEnvironmentConfigurationClient;
import br.com.portalmanager.platform.workspace.entrypoint.workspaceconfiguration.support.WorkspaceEnvironmentConfigurationFactory;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.application_scope.ApplicationScopeTypeClient;
import br.com.portalmanager.platform.workspace.entrypoint.environment.support.EnvironmentFactory;
import br.com.portalmanager.platform.workspace.entrypoint.environment.support.EnvironmentScenario;
import br.com.portalmanager.platform.workspace.entrypoint.environment.workspace.support.WorkspaceEnvironmentClient;
import br.com.portalmanager.platform.workspace.entrypoint.schema.SchemaScenario;
import br.com.portalmanager.platform.workspace.entrypoint.schema.SchemaFactory;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.CatalogBuilder;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.CatalogSetupClient;
import java.util.Objects;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Prepares an application and one workspace environment for application configuration HTTP tests. */
@Component
public final class ApplicationEnvironmentConfigurationScenario implements TestScenario<ApplicationEnvironmentConfigurationScenario> {
    private final EnvironmentScenario environments;
    private final SchemaScenario schemas;
    private final ApplicationScopeTypeClient applicationScopes;
    private final ApplicationClient applications;
    private final WorkspaceEnvironmentClient workspaceEnvironments;
    private final WorkspaceEnvironmentConfigurationClient workspaceConfigurations;
    private final CatalogSetupClient catalogs;
    private final ApplicationFactory applicationFactory = new ApplicationFactory();
    private final EnvironmentFactory environmentFactory = new EnvironmentFactory();
    private final SchemaFactory schemaFactory = new SchemaFactory();
    private final WorkspaceEnvironmentConfigurationFactory workspaceConfigurationFactory = new WorkspaceEnvironmentConfigurationFactory();
    private String applicationIdentifier;
    private String environmentIdentifier;

    public ApplicationEnvironmentConfigurationScenario(EnvironmentScenario environments, SchemaScenario schemas,
            ApplicationScopeTypeClient applicationScopes, ApplicationClient applications,
            WorkspaceEnvironmentClient workspaceEnvironments,
            WorkspaceEnvironmentConfigurationClient workspaceConfigurations,
            CatalogSetupClient catalogs) {
        this.environments=Objects.requireNonNull(environments); this.schemas=Objects.requireNonNull(schemas);
        this.applicationScopes=Objects.requireNonNull(applicationScopes); this.applications=Objects.requireNonNull(applications);
        this.workspaceEnvironments=Objects.requireNonNull(workspaceEnvironments);
        this.workspaceConfigurations=Objects.requireNonNull(workspaceConfigurations);
        this.catalogs=Objects.requireNonNull(catalogs);
    }

    @Override public ApplicationEnvironmentConfigurationScenario setup() {
        environments.setup();
        new ApplicationScenario(schemas, applicationScopes).setup();
        schemas.bindResource("PLATFORM", "application-environment-configuration",
                schemaFactory.resource("application-environment-configuration", "Application Environment Configuration",
                        "Contract for application configuration, settings and publisher parameters",
                        "Application Environment Configuration v1", "application-environment-configuration.schema.json"));
        schemas.bindResource("PLATFORM", "application-environment-configuration-settings",
                schemaFactory.resource("application-environment-configuration-settings", "Application Environment Configuration Settings",
                        "Settings contract for application environment configurations",
                        "Application Environment Configuration Settings v1", "application-environment-configuration-settings.json"));
        schemas.bindResource("PLATFORM", "workspace-environment-configuration",
                schemaFactory.resource("workspace-environment-configuration", "Workspace Environment Configuration",
                        "Contract for workspace configuration, settings and publisher parameters",
                        "Workspace Environment Configuration v1", "workspace-environment-configuration.schema.json"));
        schemas.bindResource("PLATFORM", "workspace-environment-configuration-settings",
                schemaFactory.resource("workspace-environment-configuration-settings", "Workspace Environment Configuration Settings",
                        "Settings contract for workspace environment configurations",
                        "Workspace Environment Configuration Settings v1", "workspace-environment-configuration-settings.json"));
        ensureCatalog("/api/v1/resource-scope-type", "WORKSPACE", "Workspace resource scope", 1);
        ensureCatalog("/api/v1/resource-scope-type", "APPLICATION", "Application resource scope", 2);
        var publisher = catalogs.findByCode("/api/v1/publishers", "KAFKA");
        if (publisher.statusCode() == 404) {
            var settings = JsonMapper.builder().build().createObjectNode().put("scope", "WORKSPACE");
            catalogs.create("/api/v1/publishers", CatalogBuilder.builder().withCode("KAFKA")
                    .withLabel("Kafka").withDescription("Kafka publisher for integration tests")
                    .withSortOrder(1).withSettings(settings).build()).expectCreated();
        } else {
            publisher.expect2xx();
        }
        ensurePublisher("APP_CONFIG", "APPLICATION");
        applicationIdentifier=applications.create(environments.workspaceIdentifier(), applicationFactory.valid())
                .expectCreated().extractBody().identifier();
        environmentIdentifier=workspaceEnvironments.create(environments.workspaceIdentifier(), environmentFactory.valid())
                .expectCreated().extractBody().identifier();
        workspaceConfigurations.create(environments.workspaceIdentifier(),
                workspaceConfigurationFactory.valid(environmentIdentifier)).expectCreated();
        return this;
    }

    private void ensurePublisher(String code, String scope) {
        var publisher = catalogs.findByCode("/api/v1/publishers", code);
        if (publisher.statusCode() == 404) {
            var settings = JsonMapper.builder().build().createObjectNode().put("scope", scope);
            catalogs.create("/api/v1/publishers", CatalogBuilder.builder().withCode(code)
                    .withLabel(code).withDescription("Publisher for integration tests")
                    .withSortOrder(2).withSettings(settings).build()).expectCreated();
        } else {
            publisher.expect2xx();
        }
    }

    private void ensureCatalog(String path, String code, String label, int sortOrder) {
        var existing = catalogs.findByCode(path, code);
        if (existing.statusCode() == 404) {
            catalogs.create(path, CatalogBuilder.builder().withCode(code).withLabel(label)
                    .withDescription("Resource scope for integration tests").withSortOrder(sortOrder).build()).expectCreated();
        } else {
            existing.expect2xx();
        }
    }

    public String workspaceIdentifier() { return environments.workspaceIdentifier(); }
    public String applicationIdentifier() { return applicationIdentifier; }
    public String environmentIdentifier() { return environmentIdentifier; }

    public String createEnvironmentWithoutWorkspaceConfiguration() {
        return workspaceEnvironments.create(environments.workspaceIdentifier(), environmentFactory.valid())
                .expectCreated().extractBody().identifier();
    }

    public String createAnotherApplication() {
        return applications.create(environments.workspaceIdentifier(), applicationFactory.valid())
                .expectCreated().extractBody().identifier();
    }
}
