package br.com.portalmanager.platform.workspace.integration.feature.onboarding.support;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.fixture.scenario.TestScenario;
import br.com.portalmanager.platform.workspace.integration.core.workspace.support.WorkspaceClient;
import br.com.portalmanager.platform.workspace.integration.core.workspace.support.WorkspaceFactory;
import br.com.portalmanager.platform.workspace.integration.core.workspace.support.WorkspaceScenario;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.onboardingphasetype.support.OnboardingPhaseFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.onboardingphasetype.support.OnboardingTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.workspacetype.support.WorkspaceTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.workspacetype.support.WorkspaceTypeFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaScenario;
import br.com.portalmanager.platform.workspace.integration.core.applicationconfiguration.support.ApplicationEnvironmentConfigurationClient;
import br.com.portalmanager.platform.workspace.integration.core.applicationconfiguration.support.ApplicationEnvironmentConfigurationFactory;
import br.com.portalmanager.platform.workspace.integration.core.applicationconfiguration.support.ApplicationEnvironmentConfigurationScenario;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Prepares an isolated workspace and the catalog/schema prerequisites for onboarding HTTP scenarios. */
@Component
public final class OnboardingScenario implements TestScenario<String> {
    private final AuthorizationMock authorization;
    private final WorkspaceClient workspaces;
    private final WorkspaceTypeClient workspaceTypes;
    private final OnboardingTypeClient onboardingTypes;
    private final SchemaScenario schemas;
    private final ApplicationEnvironmentConfigurationScenario applicationConfigurationsScenario;
    private final ApplicationEnvironmentConfigurationClient applicationConfigurations;

    public OnboardingScenario(AuthorizationMock authorization, WorkspaceClient workspaces,
            WorkspaceTypeClient workspaceTypes, OnboardingTypeClient onboardingTypes, SchemaScenario schemas,
            ApplicationEnvironmentConfigurationScenario applicationConfigurationsScenario,
            ApplicationEnvironmentConfigurationClient applicationConfigurations) {
        this.authorization = Objects.requireNonNull(authorization);
        this.workspaces = Objects.requireNonNull(workspaces);
        this.workspaceTypes = Objects.requireNonNull(workspaceTypes);
        this.onboardingTypes = Objects.requireNonNull(onboardingTypes);
        this.schemas = Objects.requireNonNull(schemas);
        this.applicationConfigurationsScenario = Objects.requireNonNull(applicationConfigurationsScenario);
        this.applicationConfigurations = Objects.requireNonNull(applicationConfigurations);
    }

    @Override public String setup() {
        new WorkspaceScenario(authorization, workspaceTypes, new WorkspaceTypeFactory(), onboardingTypes,
                new OnboardingPhaseFactory(), schemas).setup();
        return workspaces.create(new WorkspaceFactory().manager("Onboarding workspace"))
                .expectCreated().extractBody().identifier();
    }

    /** Prepares every onboarding phase through workspace, application, and configuration HTTP clients. */
    public String readyToComplete() {
        var scenario = applicationConfigurationsScenario.setup();
        applicationConfigurations.create(scenario.workspaceIdentifier(), scenario.applicationIdentifier(),
                new ApplicationEnvironmentConfigurationFactory().valid(scenario.environmentIdentifier()))
                .expectCreated();
        return scenario.workspaceIdentifier();
    }
}
