package br.com.portalmanager.platform.workspace.core.application.repository;

import br.com.portalmanager.platform.library.testing.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.library.testing.annotation.WithMySql;
import br.com.portalmanager.platform.workspace.core.application.domain.Application;
import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.core.workspace.repository.WorkspaceRepository;
import br.com.portalmanager.platform.workspace.foundation.catalog.applicationscopetype.domain.ApplicationScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.domain.WorkspaceTypeCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@PlatformIntegrationTest
@WithMySql
class ApplicationPersistenceIT {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private WorkspaceRepository workspaces;
    @Autowired private ApplicationRepository applications;

    @BeforeEach
    void seedCatalogs() {
        jdbc.update("INSERT IGNORE INTO type_workspaces (code, label, description, sort_order, is_active, settings) VALUES ('MANAGER', 'Manager', 'Management workspace', 2, true, '{}')");
        jdbc.update("INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings) VALUES ('ACTIVE', 'Active', 'Active state', 1, true, '{}')");
        jdbc.update("INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings) VALUES ('INACTIVE', 'Inactive', 'Inactive state', 2, true, '{}')");
        jdbc.update("INSERT IGNORE INTO type_application_scopes (code, label, description, sort_order, is_active, settings) VALUES ('BACKEND', 'Backend', 'Backend scope', 1, true, '{}')");
    }

    @Test
    void keepsNameUniqueWithinWorkspaceEvenAfterInactivation() {
        Workspace workspace = workspace("Manager A");
        Application first = applications.saveAndFlush(application(workspace, "Portal App"));
        first.inactivate(LocalDateTime.now());
        applications.saveAndFlush(first);

        assertThat(applications.findByWorkspaceAndLifecycle(workspace.getId(), LifecycleTypeCode.inactive().value()))
                .extracting(Application::getIdentifier).contains(first.getIdentifier());
        assertThrows(DataIntegrityViolationException.class,
                () -> applications.saveAndFlush(application(workspace, "Portal App")));
    }

    @Test
    void allowsSameNameInAnotherWorkspace() {
        Application first = applications.saveAndFlush(application(workspace("Manager B"), "Shared Name"));
        Application second = applications.saveAndFlush(application(workspace("Manager C"), "Shared Name"));
        assertThat(second.getIdentifier()).isNotEqualTo(first.getIdentifier());
    }

    private Workspace workspace(String name) {
        return workspaces.saveAndFlush(new Workspace(WorkspaceTypeCode.of("MANAGER"), name,
                "Workspace for applications", "requester", "MA", "{}", null,
                "workspace@portalmanager.com", LocalDateTime.now()));
    }

    private Application application(Workspace workspace, String name) {
        return new Application(workspace.getId(), name, "alias", "APP", ApplicationScopeTypeCode.of("BACKEND"), "A-APP", "{}",
                LocalDateTime.now());
    }
}
