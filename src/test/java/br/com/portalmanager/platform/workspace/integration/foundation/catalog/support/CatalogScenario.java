package br.com.portalmanager.platform.workspace.integration.foundation.catalog.support;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.testing.fixture.scenario.TestScenario;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.applicationscopetype.support.ApplicationScopeTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.lifecycletype.support.LifecycleTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.onboardingphasetype.support.OnboardingTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.schemascopetype.support.SchemaScopeTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.schemaversionstatustype.support.SchemaVersionStatusTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.workspacetype.support.WorkspaceTypeClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CatalogScenario implements TestScenario<CatalogScenario> {
    private final List<Runnable> setupSteps = new ArrayList<>();

    private CatalogScenario() {
    }

    public static CatalogScenario builder() {
        return new CatalogScenario();
    }

    public CatalogScenario withLifecycleTypes(LifecycleTypeClient client, List<CatalogDTO> types) {
        return addCatalog(client, types);
    }

    public CatalogScenario withWorkspaceTypes(WorkspaceTypeClient client, List<CatalogDTO> types) {
        return addCatalog(client, types);
    }

    public CatalogScenario withOnboardingPhases(OnboardingTypeClient client, List<CatalogDTO> phases) {
        return addCatalog(client, phases);
    }

    public CatalogScenario withApplicationScopeTypes(ApplicationScopeTypeClient client, List<CatalogDTO> types) {
        return addCatalog(client, types);
    }

    public CatalogScenario withSchemaScopes(SchemaScopeTypeClient client, List<CatalogDTO> scopes) {
        return addCatalog(client, scopes);
    }

    public CatalogScenario withSchemaVersionStatuses(
            SchemaVersionStatusTypeClient client, List<CatalogDTO> statuses) {
        return addCatalog(client, statuses);
    }

    private CatalogScenario addCatalog(CatalogClient client, List<CatalogDTO> entries) {
        Objects.requireNonNull(client, "Catalog client is required");
        Objects.requireNonNull(entries, "Catalog entries are required");
        entries.stream()
                .map(entry -> Objects.requireNonNull(entry, "Catalog entry is required"))
                .map(entry -> (Runnable) () -> {
                    var existing = client.findByCode(entry.code());
                    if (existing.statusCode() == 404) {
                        client.create(entry).expectCreated();
                    } else {
                        existing.expect2xx();
                    }
                })
                .forEach(setupSteps::add);
        return this;
    }

    @Override
    public CatalogScenario setup() {
        setupSteps.forEach(Runnable::run);
        return this;
    }
}
