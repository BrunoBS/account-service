package br.com.portalmanager.platform.workspace.feature.platform.repository;

import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.feature.platform.domain.featurecontext.FeatureContext;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@PlatformIntegrationTest
@WithMySql
class FeatureContextPersistenceIT {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private FeatureContextRepository repository;

    @BeforeEach
    void seedLifecycleTypes() {
        jdbc.update(
            "INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings) VALUES ('ACTIVE','Active','Active lifecycle state',1,true,'{}'),('INACTIVE','Inactive','Inactive lifecycle state',2,true,'{}'),('QUARANTINED','Quarantined','Quarantined lifecycle state',3,true,'{}')"
        );
    }

    @Test
    void shouldEnforceUniqueContextNameAtDatabaseLevel() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 25, 15, 30);
        repository.saveAndFlush(new FeatureContext("manager-account", "Manager Account", "Manager context", now));
        FeatureContext duplicate = new FeatureContext(
            "manager-account-alt",
            "Manager Account",
            "Duplicate manager context",
            now
        );
        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(duplicate));
    }

    @Test
    void shouldRejectStaleVersionOnUpdate() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 25, 15, 45);
        FeatureContext saved = repository.saveAndFlush(
            new FeatureContext("catalog-account", "Catalog Account", "Catalog context", now)
        );

        jdbc.update("update platform_feature_contexts set version = version + 1 where id = ?", saved.getId());
        saved.update("Catalog Account", "Outdated update", now.plusMinutes(1));

        assertThrows(ObjectOptimisticLockingFailureException.class, () -> repository.saveAndFlush(saved));
    }
}
