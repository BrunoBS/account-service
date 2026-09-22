package br.com.itau.portalmanager.workspace.core.workspace.repository;

import br.com.itau.portalmanager.workspace.core.workspace.domain.Workspace;
import br.com.itau.portalmanager.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeEnum;
import br.com.itau.portalmanager.workspace.foundation.catalog.workspacetype.domain.WorkspaceTypeEnum;
import br.com.portalmanager.platform.testing.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.testing.annotation.WithMySql;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.RollbackException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@PlatformIntegrationTest
@WithMySql
class WorkspacePersistenceIT {

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void shouldPersistWorkspaceWithApproversAndDefaultLifecycle() {
        var workspace = newWorkspace("Portal Manager", "Primeira descrição válida");
        workspace.addApprover("123456", "approver@portalmanager.com");

        var saved = workspaceRepository.saveAndFlush(workspace);
        var reloaded = workspaceRepository.findDetailedById(saved.getId()).orElseThrow();

        assertThat(reloaded.getIdentifier()).hasSize(36);
        assertThat(reloaded.getVersion()).isNotNull();
        assertThat(reloaded.getWorkspaceType()).isEqualTo(WorkspaceTypeEnum.ADMIN);
        assertThat(reloaded.getLifecycle()).isEqualTo(LifecycleTypeEnum.ACTIVE);
        assertThat(reloaded.isOnboarding()).isFalse();
        assertThat(reloaded.getApprovers()).singleElement().satisfies(approver -> {
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
        var saved = workspaceRepository.saveAndFlush(
                newWorkspace("Optimistic Workspace", "Descrição inicial válida"));
        var initialVersion = saved.getVersion();

        var firstEntityManager = entityManagerFactory.createEntityManager();
        var staleEntityManager = entityManagerFactory.createEntityManager();

        try {
            firstEntityManager.getTransaction().begin();
            staleEntityManager.getTransaction().begin();

            var first = firstEntityManager.find(Workspace.class, saved.getId());
            var stale = staleEntityManager.find(Workspace.class, saved.getId());

            first.updateDescription(
                    "Descrição alterada pela primeira transação",
                    LocalDateTime.of(2026, 9, 20, 11, 0));
            firstEntityManager.getTransaction().commit();

            stale.updateDescription(
                    "Descrição da transação obsoleta",
                    LocalDateTime.of(2026, 9, 20, 12, 0));

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
                WorkspaceTypeEnum.ADMIN,
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
