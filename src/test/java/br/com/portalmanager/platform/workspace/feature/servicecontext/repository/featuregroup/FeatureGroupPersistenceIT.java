package br.com.portalmanager.platform.workspace.feature.servicecontext.repository.featuregroup;

import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.featuregroup.FeatureGroup;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.CatalogScenario;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.lifecycle.LifecycleTypeClient;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.lifecycle.LifecycleTypeFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDateTime;

class FeatureGroupPersistenceIT  extends br.com.portalmanager.platform.workspace.support.WorkspaceIntegrationTestSupport{

    @Autowired private FeatureGroupRepository repository;

    @Autowired private LifecycleTypeClient lifecycles;
    private final LifecycleTypeFactory lifecycleTypeFactory = new LifecycleTypeFactory();

    @BeforeEach
    void seedLifecycleTypes() {
        CatalogScenario.builder()
                .withLifecycleTypes(lifecycles, lifecycleTypeFactory.all())
                .setup();
    }

    @Test
    void shouldEnforceUniqueContextNameAtDatabaseLevel() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 25, 15, 30);
        repository.saveAndFlush(
                new FeatureGroup("manager-account", "Manager Account", "Manager group", now));
        FeatureGroup duplicate =
                new FeatureGroup("manager-account-alt", "Manager Account", "Duplicate manager group", now);
        assertThrows(
                DataIntegrityViolationException.class, () -> repository.saveAndFlush(duplicate));
    }

    @Test
    void shouldRejectStaleVersionOnUpdate() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 25, 15, 45);
        FeatureGroup saved =
                repository.saveAndFlush(
                        new FeatureGroup("catalog-account", "Catalog Account", "Catalog group", now));

        FeatureGroup current = repository.findById(saved.getId()).orElseThrow();
        current.update("Catalog Account", "Current update", now.plusMinutes(1));
        repository.saveAndFlush(current);
        saved.update("Catalog Account", "Outdated update", now.plusMinutes(1));

        assertThrows(
                ObjectOptimisticLockingFailureException.class,
                () -> repository.saveAndFlush(saved));
    }
}
