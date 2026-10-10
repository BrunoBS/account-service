package br.com.portalmanager.platform.workspace.core.workspace.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.domain.WorkspaceTypeCode;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.RollbackException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@PlatformIntegrationTest
@WithMySql
class WorkspacePersistenceIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @org.junit.jupiter.api.BeforeEach
    void seedWorkspaceTypeCatalog() {
        seedWorkspaceTypes();
        seedLifecycleTypes();
    }

    private void seedWorkspaceTypes() {
        jdbcTemplate.update(
            "INSERT IGNORE INTO type_workspaces (code, label, description, sort_order, is_active, settings) VALUES ('ADMIN', 'Admin', 'Administrative workspace', 1, true, '{}')"
        );
        jdbcTemplate.update(
            "INSERT IGNORE INTO type_workspaces (code, label, description, sort_order, is_active, settings) VALUES ('MANAGER', 'Manager', 'Management workspace', 2, true, '{}')"
        );
        jdbcTemplate.update(
            "INSERT IGNORE INTO type_workspaces (code, label, description, sort_order, is_active, settings) VALUES ('CATALOG', 'Catalog', 'Catalog workspace', 3, true, '{}')"
        );
    }

    private void seedLifecycleTypes() {
        jdbcTemplate.update(
            "INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings) VALUES ('ACTIVE', 'Active', 'Active lifecycle state', 1, true, '{}')"
        );
        jdbcTemplate.update(
            "INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings) VALUES ('INACTIVE', 'Inactive', 'Inactive lifecycle state', 2, true, '{}')"
        );
        jdbcTemplate.update(
            "INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings) VALUES ('QUARANTINED', 'Quarantined', 'Quarantined lifecycle state', 3, true, '{}')"
        );
    }

    @Test
    void shouldPersistWorkspaceWithApproversAndDefaultLifecycle() {
        var workspace = newWorkspace("Portal Manager", "Primeira descrição válida");
        workspace.addApprover("123456", "approver@portalmanager.com");

        var saved = workspaceRepository.saveAndFlush(workspace);
        var reloaded = workspaceRepository.findDetailedById(saved.getId()).orElseThrow();

        assertThat(reloaded.getIdentifier()).hasSize(36);
        assertThat(reloaded.getVersion()).isNotNull();
        assertThat(reloaded.getWorkspaceType()).isEqualTo(WorkspaceTypeCode.of("ADMIN"));
        assertThat(reloaded.getLifecycle()).isEqualTo(LifecycleTypeCode.active());
        assertThat(reloaded.isOnboarding()).isFalse();
        assertThat(reloaded.getApprovers())
            .singleElement()
            .satisfies(approver -> {
                assertThat(approver.getFunctional()).isEqualTo("123456");
                assertThat(approver.getEmail()).isEqualTo("approver@portalmanager.com");
            });
    }

    @Test
    void shouldEnforceUniqueWorkspaceNameRegardlessOfLifecycle() {
        workspaceRepository.saveAndFlush(newWorkspace("Unique Workspace", "Descrição válida número um"));

        var duplicate = newWorkspace("Unique Workspace", "Descrição válida número dois");

        assertThrows(DataIntegrityViolationException.class, () -> workspaceRepository.saveAndFlush(duplicate));
    }

    @Test
    void shouldRejectStaleUpdateUsingJpaVersion() {
        var saved = workspaceRepository.saveAndFlush(newWorkspace("Optimistic Workspace", "Descrição inicial válida"));
        var initialVersion = saved.getVersion();

        var firstEntityManager = entityManagerFactory.createEntityManager();
        var staleEntityManager = entityManagerFactory.createEntityManager();

        try {
            firstEntityManager.getTransaction().begin();
            staleEntityManager.getTransaction().begin();

            var first = firstEntityManager.find(Workspace.class, saved.getId());
            var stale = staleEntityManager.find(Workspace.class, saved.getId());

            first.updateDescription("Descrição alterada pela primeira transação", LocalDateTime.of(2026, 9, 20, 11, 0));
            firstEntityManager.getTransaction().commit();

            stale.updateDescription("Descrição da transação obsoleta", LocalDateTime.of(2026, 9, 20, 12, 0));

            assertThrows(RollbackException.class, staleEntityManager.getTransaction()::commit);
        } finally {
            firstEntityManager.close();
            staleEntityManager.close();
        }

        var reloaded = workspaceRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getDescription()).isEqualTo("Descrição alterada pela primeira transação");
        assertThat(reloaded.getVersion()).isGreaterThan(initialVersion);
    }

    private Workspace newWorkspace(String name, String description) {
        return new Workspace(
            WorkspaceTypeCode.of("ADMIN"),
            name,
            description,
            "requester",
            "PM",
            "{\"theme\":\"default\"}",
            null,
            "workspace@portalmanager.com",
            LocalDateTime.of(2026, 9, 20, 10, 0)
        );
    }
}
