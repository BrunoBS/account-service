package br.com.itau.portalmanager.workspace.feature.platform.repository;

import br.com.itau.portalmanager.workspace.feature.platform.domain.FeatureContext;
import br.com.portalmanager.platform.testing.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.testing.annotation.WithMySql;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;

@PlatformIntegrationTest
@WithMySql
class FeatureContextPersistenceIT {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private FeatureContextRepository repository;

    @BeforeEach
    void seedLifecycleTypes() {
        jdbc.update("INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings) VALUES ('ACTIVE','Active','Active lifecycle state',1,true,'{}'),('INACTIVE','Inactive','Inactive lifecycle state',2,true,'{}'),('QUARANTINED','Quarantined','Quarantined lifecycle state',3,true,'{}')");
    }

    @Test
    void shouldEnforceUniqueContextNameAtDatabaseLevel() {
        var now = LocalDateTime.of(2026, 9, 25, 15, 30);
        repository.saveAndFlush(new FeatureContext("MANAGER_ACCOUNT", "MANAGER_ACCOUNT", "Manager context", now));
        var duplicate = new FeatureContext("MANAGER_ACCOUNT_ALT", "MANAGER_ACCOUNT", "Duplicate manager context", now);
        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(duplicate));
    }

    @Test
    void shouldRejectStaleVersionOnUpdate() {
        var now = LocalDateTime.of(2026, 9, 25, 15, 45);
        var saved = repository.saveAndFlush(
                new FeatureContext("CATALOG_ACCOUNT", "CATALOG_ACCOUNT", "Catalog context", now)
        );

        jdbc.update(
                "update platform_feature_contexts set version = version + 1 where id = ?",
                saved.getId()
        );
        saved.update("CATALOG_ACCOUNT", "Outdated update", now.plusMinutes(1));

        assertThrows(
                ObjectOptimisticLockingFailureException.class,
                () -> repository.saveAndFlush(saved)
        );
    }
}
