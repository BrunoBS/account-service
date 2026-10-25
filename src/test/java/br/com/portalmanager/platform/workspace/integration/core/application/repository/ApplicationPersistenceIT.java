package br.com.portalmanager.platform.workspace.integration.core.application.repository;


import br.com.portalmanager.platform.workspace.core.application.repository.ApplicationRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.core.application.domain.Application;
import br.com.portalmanager.platform.workspace.core.application.domain.ApplicationDetails;
import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.core.workspace.domain.WorkspaceDetails;
import br.com.portalmanager.platform.workspace.core.workspace.repository.WorkspaceRepository;
import br.com.portalmanager.platform.workspace.foundation.catalog.applicationscopetype.domain.ApplicationScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.domain.WorkspaceTypeCode;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.support.CatalogScenario;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.applicationscopetype.support.ApplicationScopeTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.applicationscopetype.support.CatalogFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.lifecycletype.support.LifecycleTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.lifecycletype.support.LifecycleTypeFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.workspacetype.support.WorkspaceTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.workspacetype.support.WorkspaceTypeFactory;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class ApplicationPersistenceIT  extends br.com.portalmanager.platform.workspace.integration.support.WorkspaceIntegrationTestSupport{

    @Autowired
    private WorkspaceRepository workspaces;

    @Autowired
    private ApplicationRepository applications;

    @Autowired private WorkspaceTypeClient workspaceTypes;
    @Autowired private LifecycleTypeClient lifecycles;
    @Autowired private ApplicationScopeTypeClient applicationScopes;

    private final WorkspaceTypeFactory workspaceTypeFactory = new WorkspaceTypeFactory();
    private final LifecycleTypeFactory lifecycleTypeFactory = new LifecycleTypeFactory();
    private final CatalogFactory applicationScopeFactory = new CatalogFactory();

    @BeforeEach
    void seedCatalogs() {
        CatalogScenario.builder()
            .withWorkspaceTypes(workspaceTypes, java.util.List.of(workspaceTypeFactory.manager()))
            .withLifecycleTypes(lifecycles, lifecycleTypeFactory.all())
            .withApplicationScopeTypes(applicationScopes,
                java.util.List.of(applicationScopeFactory.withLabel("BACKEND", "Backend")))
            .setup();
    }

    @Test
    void keepsNameUniqueWithinWorkspaceEvenAfterInactivation() {
        Workspace workspace = workspace("Manager A");
        Application first = applications.saveAndFlush(application(workspace, "Portal App"));
        first.inactivate(LocalDateTime.now());
        applications.saveAndFlush(first);

        assertThat(
            applications
                .findFiltered(
                    workspace.getId(),
                    LifecycleTypeCode.inactive().value(),
                    null,
                    org.springframework.data.domain.PageRequest.of(0, 20)
                )
                .getContent()
        )
            .extracting(Application::getIdentifier)
            .contains(first.getIdentifier());
        assertThrows(DataIntegrityViolationException.class, () ->
            applications.saveAndFlush(application(workspace, "portal app"))
        );
    }

    @Test
    void allowsSameNameInAnotherWorkspace() {
        Application first = applications.saveAndFlush(application(workspace("Manager B"), "Shared Name"));
        Application second = applications.saveAndFlush(application(workspace("Manager C"), "Shared Name"));
        assertThat(second.getIdentifier()).isNotEqualTo(first.getIdentifier());
    }

    private Workspace workspace(String name) {
        return workspaces.saveAndFlush(
            new Workspace(
                new WorkspaceDetails(
                    WorkspaceTypeCode.of("MANAGER"),
                    name,
                    "Workspace for applications",
                    "requester",
                    "MA",
                    tools.jackson.databind.json.JsonMapper.builder().build().createObjectNode(),
                    null,
                    "workspace@portalmanager.com"
                ),
                LocalDateTime.now()
            )
        );
    }

    private Application application(Workspace workspace, String name) {
        return new Application(
            workspace.getId(),
            new ApplicationDetails(
                name,
                "alias",
                null,
                "APP",
                ApplicationScopeTypeCode.of("BACKEND"),
                "A-APP",
                tools.jackson.databind.json.JsonMapper.builder().build().createObjectNode()
            ),
            LocalDateTime.now()
        );
    }
}
