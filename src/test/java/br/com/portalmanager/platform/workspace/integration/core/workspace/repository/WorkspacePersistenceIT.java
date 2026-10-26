package br.com.portalmanager.platform.workspace.core.workspace.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.portalmanager.platform.library.authorization.model.ParsedGroup;
import br.com.portalmanager.platform.library.authorization.model.UserContext;
import br.com.portalmanager.platform.library.authorization.model.UserSession;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.core.workspace.domain.WorkspaceDetails;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceFinder;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.domain.WorkspaceTypeCode;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.lifecycletype.support.LifecycleTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.lifecycletype.support.LifecycleTypeFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.support.CatalogScenario;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.workspacetype.support.WorkspaceTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.workspacetype.support.WorkspaceTypeFactory;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.RollbackException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

class WorkspacePersistenceIT extends BaseIntegrationTest {

    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private WorkspaceTypeClient workspaceTypes;
    @Autowired private LifecycleTypeClient lifecycles;
    @Autowired private WorkspaceFinder workspaceFinder;
    @Autowired private br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceQueryService workspaceQueries;

    private final WorkspaceTypeFactory workspaceTypeFactory = new WorkspaceTypeFactory();
    private final LifecycleTypeFactory lifecycleTypeFactory = new LifecycleTypeFactory();

    @BeforeEach
    void seedCatalogs() {
        CatalogScenario.builder()
            .withWorkspaceTypes(workspaceTypes, workspaceTypeFactory.all())
            .withLifecycleTypes(lifecycles, lifecycleTypeFactory.all())
            .setup();
    }

    @Test
    void persistsWorkspaceWithApproversAndDefaultLifecycle() {
        Workspace workspace = newWorkspace("Portal Manager", "Primeira descrição válida");
        workspace.addApprover("123456", "approver@portalmanager.com");

        Workspace saved = workspaceRepository.saveAndFlush(workspace);
        Workspace reloaded = workspaceRepository.findByIdentifier(saved.getIdentifier()).orElseThrow();

        assertThat(reloaded.getIdentifier()).hasSize(36);
        assertThat(reloaded.getVersion()).isNotNull();
        assertThat(reloaded.getWorkspaceType()).isEqualTo(WorkspaceTypeCode.of("ADMIN"));
        assertThat(reloaded.getLifecycle()).isEqualTo(LifecycleTypeCode.active());
        assertThat(reloaded.isOnboarding()).isFalse();
        assertThat(reloaded.getApprovers()).singleElement().satisfies(approver -> {
            assertThat(approver.getFunctional()).isEqualTo("123456");
            assertThat(approver.getEmail()).isEqualTo("approver@portalmanager.com");
        });
    }

    @Test
    @Transactional
    void shouldPersistJsonSettingsAndProtectAgainstExternalMutation() {
        var initialSettings = JsonMapper.builder().build().createObjectNode().put("theme", "default");
        Workspace workspace = new Workspace(
            details("JSON Settings", "Descrição válida para configurações JSON", initialSettings),
            LocalDateTime.now());
        workspaceRepository.saveAndFlush(workspace);
        entityManager.clear();
        workspace = workspaceRepository.findById(workspace.getId()).orElseThrow();
        assertThat(workspace.getSettings().get("theme").asText()).isEqualTo("default");
        var settings = JsonMapper.builder().build().createObjectNode();
        settings.putObject("preferences").put("enabled", true);
        workspace.update(details(workspace.getName(), "Descrição válida para configurações JSON", settings),
            LocalDateTime.now());
        settings.put("unexpected", true);
        ((tools.jackson.databind.node.ObjectNode) workspace.getSettings()).put("external", true);
        workspaceRepository.flush();
        entityManager.clear();
        workspace = workspaceRepository.findById(workspace.getId()).orElseThrow();
        assertThat(workspace.getSettings().get("preferences").get("enabled").asBoolean()).isTrue();
        assertThat(workspace.getSettings().has("unexpected")).isFalse();
        assertThat(workspace.getSettings().has("external")).isFalse();
        workspace.update(details(workspace.getName(), workspace.getDescription(),
            tools.jackson.databind.node.NullNode.getInstance()), LocalDateTime.now());
        workspaceRepository.flush();
        entityManager.clear();
        assertThat(workspaceRepository.findById(workspace.getId()).orElseThrow().getSettings()).isNull();
    }

    @Test
    @Transactional
    void shouldApplyVisibilityWhenResolvingActiveReferencesInBothDirections() {
        Workspace workspace = new Workspace(
            new WorkspaceDetails(WorkspaceTypeCode.of("ADMIN"), "Protected Reference",
                "Descrição válida da referência", "requester", "REF", null, "TEAM_B", "workspace@example.com"),
            LocalDateTime.now());
        workspaceRepository.saveAndFlush(workspace);
        entityManager.clear();
        UserSession session = new UserSession();
        session.setGroups(Set.of("USER"));
        session.setAuthorizerGroups(Set.of(new ParsedGroup("GRP_WORKSPACE_DEV_TEAM_A", "DEV", "DEV", "TEAM_A")));
        UserContext.set(session);
        try {
            assertThrows(NotFoundException.class, () -> workspaceFinder.findActiveReference(workspace.getIdentifier()));
            assertThrows(NotFoundException.class, () -> workspaceFinder.findActiveReference(workspace.getId()));
            assertThrows(NotFoundException.class, () ->
                workspaceQueries.findReferenceIdentifierByInternalId(workspace.getId()));
            assertThat(workspaceQueries.findReferenceIdentifiersByInternalIds(List.of(workspace.getId()))).isEmpty();
            assertThat(workspaceQueries.findActiveIdentifiersByInternalIds(List.of(workspace.getId()))).isEmpty();
            session.setAuthorizerGroups(Set.of(new ParsedGroup("GRP_WORKSPACE_DEV_TEAM_B", "DEV", "DEV", "TEAM_B")));
            assertThat(workspaceFinder.findActiveReference(workspace.getIdentifier()).getId()).isEqualTo(workspace.getId());
            assertThat(workspaceFinder.findActiveReference(workspace.getId()).getIdentifier()).isEqualTo(workspace.getIdentifier());
            assertThat(workspaceQueries.findReferenceIdentifierByInternalId(workspace.getId())).isEqualTo(workspace.getIdentifier());
            assertThat(workspaceQueries.findActiveIdentifiersByInternalIds(List.of(workspace.getId())))
                .containsEntry(workspace.getId(), workspace.getIdentifier());
            session.setGroups(Set.of("PM5_OWNER"));
            session.setAuthorizerGroups(Set.of());
            assertThat(workspaceFinder.findActiveReference(workspace.getIdentifier()).getId()).isEqualTo(workspace.getId());
            assertThat(workspaceFinder.findActiveReference(workspace.getId()).getIdentifier()).isEqualTo(workspace.getIdentifier());
        } finally {
            UserContext.clear();
        }
    }

    @Test
    @Transactional
    void shouldPageEntitiesInTheDatabaseAndLoadApproversInOneBatch() {
        for (int index = 0; index < 6; index++) {
            Workspace workspace = newWorkspace("Workspace Batch " + index, "Workspace batch pagination test");
            workspace.addApprover("F1", "first@example.com");
            workspace.addApprover("F2", "second@example.com");
            workspaceRepository.save(workspace);
        }
        workspaceRepository.flush();
        entityManager.clear();
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        boolean statisticsEnabled = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        try {
            var page = workspaceRepository.findFiltered("ACTIVE", null, null,
                org.springframework.data.domain.PageRequest.of(0, 3,
                    org.springframework.data.domain.Sort.by("id")));
            assertThat(page.getTotalElements()).isEqualTo(6);
            assertThat(page.getContent()).hasSize(3);
            assertThat(statistics.getEntityLoadCount()).isEqualTo(3);
            for (Workspace item : page) assertThat(item.getApprovers()).hasSize(2);
            assertThat(statistics.getCollectionFetchCount()).isEqualTo(1);
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
        } finally {
            statistics.setStatisticsEnabled(statisticsEnabled);
        }
    }

    @Test
    void shouldResolveActiveAndHistoricalReferencesWithoutLoadingEntities() {
        Workspace active = newWorkspace("Active Reference", "Descrição válida da referência");
        active.addApprover("F1000", "approver@example.com");
        Workspace inactive = newWorkspace("Inactive Reference", "Descrição válida da referência");
        inactive.inactivate(LocalDateTime.now());
        Workspace quarantined = newWorkspace("Quarantined Reference", "Descrição válida da referência");
        quarantined.inactivate(LocalDateTime.now());
        quarantined.quarantine(LocalDateTime.now());
        workspaceRepository.saveAllAndFlush(List.of(active, inactive, quarantined));
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        boolean statisticsEnabled = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        try {
            assertThat(workspaceRepository.findActiveReferenceByIdentifier(active.getIdentifier()))
                .get().satisfies(reference -> {
                    assertThat(reference.getId()).isEqualTo(active.getId());
                    assertThat(reference.getWorkspaceType()).isEqualTo("ADMIN");
                });
            assertThat(workspaceRepository.findActiveReferenceByIdentifier(inactive.getIdentifier())).isEmpty();
            assertThat(workspaceRepository.findActiveReferenceByIdentifier(quarantined.getIdentifier())).isEmpty();
            assertThat(workspaceRepository.findReferenceById(active.getId(), "ACTIVE")).isPresent();
            assertThat(workspaceRepository.findReferenceById(inactive.getId(), "ACTIVE")).isEmpty();
            List<Long> ids = List.of(active.getId(), inactive.getId(), quarantined.getId());
            assertThat(workspaceRepository.findReferencesByIds(ids, "ACTIVE")).hasSize(1);
            assertThat(workspaceRepository.findReferencesByIds(ids, null)).hasSize(3);
            assertThat(workspaceRepository.findReferenceById(quarantined.getId(), null)).isPresent();
            assertThat(statistics.getEntityLoadCount()).isZero();
            assertThat(statistics.getCollectionLoadCount()).isZero();
        } finally {
            statistics.setStatisticsEnabled(statisticsEnabled);
        }
    }

    @Test
    void enforcesUniqueWorkspaceNameRegardlessOfLifecycle() {
        workspaceRepository.saveAndFlush(newWorkspace("Unique Workspace", "Descrição válida número um"));
        Workspace duplicate = newWorkspace("Unique Workspace", "Descrição válida número dois");
        assertThrows(DataIntegrityViolationException.class, () -> workspaceRepository.saveAndFlush(duplicate));
    }

    @Test
    void rejectsStaleUpdateUsingJpaVersion() {
        Workspace saved = workspaceRepository.saveAndFlush(
            newWorkspace("Optimistic Workspace", "Descrição inicial válida"));
        Long initialVersion = saved.getVersion();

        var firstEntityManager = entityManagerFactory.createEntityManager();
        var staleEntityManager = entityManagerFactory.createEntityManager();
        try {
            firstEntityManager.getTransaction().begin();
            staleEntityManager.getTransaction().begin();
            Workspace first = firstEntityManager.find(Workspace.class, saved.getId());
            Workspace stale = staleEntityManager.find(Workspace.class, saved.getId());

            first.update(details("Descrição alterada pela primeira transação"),
                LocalDateTime.of(2026, 9, 20, 11, 0));
            firstEntityManager.getTransaction().commit();

            stale.update(details("Descrição da transação obsoleta"), LocalDateTime.of(2026, 9, 20, 12, 0));
            assertThrows(RollbackException.class, staleEntityManager.getTransaction()::commit);
        } finally {
            firstEntityManager.close();
            staleEntityManager.close();
        }

        Workspace reloaded = workspaceRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getDescription()).isEqualTo("Descrição alterada pela primeira transação");
        assertThat(reloaded.getVersion()).isGreaterThan(initialVersion);
    }

    private Workspace newWorkspace(String name, String description) {
        return new Workspace(details(name, description), LocalDateTime.of(2026, 9, 20, 10, 0));
    }

    private WorkspaceDetails details(String description) {
        return details("Optimistic Workspace", description);
    }

    private WorkspaceDetails details(String name, String description,
            tools.jackson.databind.JsonNode settings) {
        return new WorkspaceDetails(WorkspaceTypeCode.of("ADMIN"), name, description, "requester", "PM",
            settings, null, "workspace@portalmanager.com");
    }

    private WorkspaceDetails details(String name, String description) {
        return new WorkspaceDetails(WorkspaceTypeCode.of("ADMIN"), name, description, "requester", "PM",
            JsonMapper.builder().build().createObjectNode(), null, "workspace@portalmanager.com");
    }
}
