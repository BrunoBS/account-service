package br.com.portalmanager.platform.workspace.feature.platform.repository;

import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.feature.FeatureQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@PlatformIntegrationTest
@WithMySql
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PlatformQueryIT {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private FeatureQueryService queryService;

    @Test
    void shouldReturnContextsFullyMaterializedOutsideRepositoryTransaction() {
        jdbc.update("""
                insert into type_life_cycle (code, label, description, sort_order, is_active, settings)
                values ('ACTIVE', 'Active', 'Active lifecycle state', 1, true, '{}')
                on duplicate key update code = values(code)
                """);
        jdbc.update("""
                insert into platform_microservices
                    (version, identifier, code, name, description, settings, lifecycle_code, created_at, updated_at)
                values
                    (0, '11111111-1111-1111-1111-111111111111', 'audit-service',
                     'Audit Service', 'Audit owner', '{}', 'ACTIVE', now(), now())
                """);
        jdbc.update("""
                insert into platform_features
                    (version, identifier, code, name, description, microservice_id, lifecycle_code, settings, created_at, updated_at)
                select
                    0, '22222222-2222-2222-2222-222222222222', 'audit', 'Audit',
                    'Audit feature', id, 'ACTIVE', '{}', now(), now()
                  from platform_microservices
                 where code = 'audit-service'
                """);
        jdbc.update("""
                insert into platform_feature_contexts
                    (version, identifier, code, name, description, lifecycle_code, created_at, updated_at)
                values
                    (0, '33333333-3333-3333-3333-333333333333', 'administration',
                     'Administration', 'Administration context', 'ACTIVE', now(), now())
                """);
        jdbc.update("""
                insert into platform_feature_context_relations (feature_id, feature_context_id)
                select f.id, c.id
                  from platform_features f
                  join platform_feature_contexts c on c.code = 'administration'
                 where f.code = 'audit'
                """);

        var contexts = queryService.findContexts("22222222-2222-2222-2222-222222222222");

        assertThat(contexts)
                .hasSize(1)
                .first()
                .satisfies(context -> {
                    assertThat(context.code()).isEqualTo("administration");
                    assertThat(context.name()).isEqualTo("Administration");
                });
    }
}