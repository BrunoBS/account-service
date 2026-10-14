package br.com.portalmanager.platform.workspace.feature.platform.repository.featurecontext;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.context.ContextOutput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.feature.FeatureOutput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.feature.FeatureQueryService;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.context.ContextQueryService;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@PlatformIntegrationTest
@WithMySql
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FeatureContextQueryIT {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private FeatureQueryService queryService;

    @Autowired
    private ContextQueryService contextQueryService;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void linkedFeaturesUseTheSameFilterForContentAndCountWithoutLoadingEachParentSeparately() {
        jdbc.update("INSERT IGNORE INTO type_life_cycle (code,label,sort_order,is_active,settings) VALUES ('ACTIVE','Active',1,true,'{}'),('INACTIVE','Inactive',2,true,'{}')");
        jdbc.update("INSERT INTO platform_microservices (identifier,code,name,settings,lifecycle_code,created_at,updated_at) VALUES (UUID(),'parent-service','Parent service','{}','ACTIVE',NOW(),NOW())");
        jdbc.update("INSERT INTO platform_feature_contexts (identifier,code,name,lifecycle_code,created_at,updated_at) VALUES ('33333333-3333-3333-3333-333333333333','review-context','Review context','ACTIVE',NOW(),NOW())");
        for (int index = 0; index < 4; index++) {
            jdbc.update("INSERT INTO platform_features (identifier,code,name,microservice_id,settings,lifecycle_code,created_at,updated_at) SELECT UUID(),?,?,id,'{}',?,NOW(),NOW() FROM platform_microservices", "feature-" + index, "Feature " + index, index == 3 ? "INACTIVE" : "ACTIVE");
        }
        jdbc.update("INSERT INTO platform_feature_context_relations (feature_id,feature_context_id) SELECT feature.id,context.id FROM platform_features feature CROSS JOIN platform_feature_contexts context");
        org.hibernate.stat.Statistics statistics = entityManagerFactory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        boolean originallyEnabled = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        try {
            org.springframework.data.domain.Page<FeatureOutput> result = queryService.findAll(
                "33333333-3333-3333-3333-333333333333", null, org.springframework.data.domain.PageRequest.of(0, 2));
            assertThat(result.getTotalElements()).isEqualTo(3);
            assertThat(result.getContent()).hasSize(2).allSatisfy(feature -> {
                assertThat(feature.lifecycle()).isEqualTo("ACTIVE");
                assertThat(feature.microserviceIdentifier()).isNotBlank();
            });
            // Resource existence, page contents (including parent), and matching count.
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
        } finally {
            statistics.setStatisticsEnabled(originallyEnabled);
        }
    }

    @Test
    void shouldReturnContextsFullyMaterializedOutsideRepositoryTransaction() {
        jdbc.update(
            """
            insert into type_life_cycle (code, label, description, sort_order, is_active, settings)
            values ('ACTIVE', 'Active', 'Active lifecycle state', 1, true, '{}')
            on duplicate key update code = values(code)
            """
        );
        jdbc.update(
            """
            insert into platform_microservices
                (version, identifier, code, name, description, settings, lifecycle_code, created_at, updated_at)
            values
                (0, '11111111-1111-1111-1111-111111111111', 'audit-service',
                 'Audit Service', 'Audit owner', '{}', 'ACTIVE', now(), now())
            """
        );
        jdbc.update(
            """
            insert into platform_features
                (version, identifier, code, name, description, microservice_id, lifecycle_code, settings, created_at, updated_at)
            select
                0, '22222222-2222-2222-2222-222222222222', 'audit', 'Audit',
                'Audit feature', id, 'ACTIVE', '{}', now(), now()
              from platform_microservices
             where code = 'audit-service'
            """
        );
        jdbc.update(
            """
            insert into platform_feature_contexts
                (version, identifier, code, name, description, lifecycle_code, created_at, updated_at)
            values
                (0, '33333333-3333-3333-3333-333333333333', 'administration',
                 'Administration', 'Administration context', 'ACTIVE', now(), now())
            """
        );
        jdbc.update(
            """
            insert into platform_feature_context_relations (feature_id, feature_context_id)
            select f.id, c.id
              from platform_features f
              join platform_feature_contexts c on c.code = 'administration'
             where f.code = 'audit'
            """
        );

        List<ContextOutput> contexts = contextQueryService.findAll(" 22222222-2222-2222-2222-222222222222 ", null, org.springframework.data.domain.PageRequest.of(0, 20)).getContent();

        assertThat(contexts)
            .hasSize(1)
            .first()
            .satisfies(context -> {
                assertThat(context.code()).isEqualTo("administration");
                assertThat(context.name()).isEqualTo("Administration");
            });
    }
}
