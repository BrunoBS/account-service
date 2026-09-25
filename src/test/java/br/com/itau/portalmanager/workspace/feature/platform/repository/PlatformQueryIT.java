package br.com.itau.portalmanager.workspace.feature.platform.repository;

import br.com.itau.portalmanager.workspace.feature.platform.usecase.feature.FeatureQueryService;
import br.com.portalmanager.platform.testing.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.testing.annotation.WithMySql;
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
    void shouldReturnScopesFullyMaterializedOutsideRepositoryTransaction() {
        jdbc.update("""
                insert into type_life_cycle (code, label, description, sort_order, is_active, settings)
                values ('ACTIVE', 'Active', 'Active lifecycle state', 1, true, '{}')
                on duplicate key update code = values(code)
                """);
        jdbc.update("""
                insert into type_feature_scopes (code, label, description, sort_order, is_active, settings)
                values ('ADMINISTRATION', 'Administration', 'Administrative scope', 1, true, '{}')
                on duplicate key update code = values(code)
                """);
        jdbc.update("""
                insert into platform_services
                    (version, identifier, code, name, description, lifecycle_code, created_at, updated_at)
                values
                    (0, '11111111-1111-1111-1111-111111111111', 'audit-service',
                     'Audit Service', 'Audit owner', 'ACTIVE', now(), now())
                """);
        jdbc.update("""
                insert into platform_features
                    (version, identifier, code, name, description, service_id, lifecycle_code, settings, created_at, updated_at)
                select
                    0, '22222222-2222-2222-2222-222222222222', 'AUDIT', 'Audit',
                    'Audit feature', id, 'ACTIVE', '{}', now(), now()
                  from platform_services
                 where code = 'audit-service'
                """);
        jdbc.update("""
                insert into platform_feature_scopes (feature_id, feature_scope_code)
                select id, 'ADMINISTRATION'
                  from platform_features
                 where code = 'AUDIT'
                """);

        var scopes = queryService.findScopes("22222222-2222-2222-2222-222222222222");

        assertThat(scopes)
                .hasSize(1)
                .first()
                .satisfies(scope -> {
                    assertThat(scope.code()).isEqualTo("ADMINISTRATION");
                    assertThat(scope.active()).isTrue();
                });
    }
}
