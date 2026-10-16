package br.com.portalmanager.platform.workspace.feature.servicecontext.repository;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.support.MigrationSchemaValidationTestConfiguration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

@PlatformIntegrationTest
@WithMySql
@Import(MigrationSchemaValidationTestConfiguration.class)
@TestPropertySource(
        properties = {
            "spring.flyway.target=77",
            "platform.schema-validation.enabled=false",
            "spring.jpa.hibernate.ddl-auto=none"
        })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ServiceContextRenameMigrationIT {
    @Autowired private DataSource dataSource;

    @Test
    void preservesMicroserviceFeatureGroupAndAssociationWithStableVersions() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update(
                "INSERT IGNORE INTO type_life_cycle(code,label,sort_order,is_active,settings)"
                    + " VALUES ('ACTIVE','Active',1,true,'{}')");
        jdbc.update(
                "INSERT IGNORE INTO type_schema_scopes(code,label,sort_order,is_active,settings)"
                    + " VALUES ('PLATFORM','Platform',1,true,'{}')");
        jdbc.update(
                "INSERT IGNORE INTO"
                    + " type_schema_version_status(code,label,sort_order,is_active,settings) VALUES"
                    + " ('PUBLISHED','Published',1,true,'{}')");
        jdbc.execute(
                (org.springframework.jdbc.core.ConnectionCallback<Void>)
                        connection -> {
                            org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(
                                    connection,
                                    new org.springframework.core.io.ClassPathResource(
                                            "db/migration/V64__context_resource_schema.sql"));
                            org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(
                                    connection,
                                    new org.springframework.core.io.ClassPathResource(
                                            "db/migration/V65__separate_feature_context_resource_schema.sql"));
                            return null;
                        });

        jdbc.update(
                "INSERT IGNORE INTO type_life_cycle(code,label,sort_order,is_active,settings)"
                        + " VALUES ('ACTIVE','Active',1,true,'{}')");
        jdbc.update(
                "INSERT INTO"
                    + " platform_microservices(identifier,version,code,name,settings,lifecycle_code,created_at,updated_at)"
                    + " VALUES ('rename-service',3,'rename-service','Rename"
                    + " Service','{}','ACTIVE',NOW(6),NOW(6))");
        jdbc.update(
                "INSERT INTO"
                    + " platform_features(identifier,version,code,name,microservice_id,settings,lifecycle_code,shareable,created_at,updated_at)"
                    + " SELECT 'rename-feature',4,'rename-feature','Rename"
                    + " Feature',id,'{}','ACTIVE',true,NOW(6),NOW(6) FROM platform_microservices"
                    + " WHERE identifier='rename-service'");
        jdbc.update(
                "INSERT INTO"
                    + " platform_feature_contexts(identifier,version,code,name,lifecycle_code,created_at,updated_at)"
                    + " VALUES ('rename-group',5,'rename-group','Rename"
                    + " Group','ACTIVE',NOW(6),NOW(6))");
        jdbc.update(
                "INSERT INTO platform_feature_context_relations(feature_id,feature_context_id)"
                    + " SELECT f.id,g.id FROM platform_features f JOIN platform_feature_contexts g"
                    + " ON g.identifier='rename-group' WHERE f.identifier='rename-feature'");
        java.util.List<java.util.Map<String, Object>> services =
                jdbc.queryForList(
                        "SELECT * FROM platform_microservices WHERE identifier='rename-service'");
        java.util.List<java.util.Map<String, Object>> features =
                jdbc.queryForList(
                        "SELECT * FROM platform_features WHERE identifier='rename-feature'");
        java.util.List<java.util.Map<String, Object>> groups =
                jdbc.queryForList(
                        "SELECT * FROM platform_feature_contexts WHERE identifier='rename-group'");
        java.util.List<java.util.Map<String, Object>> associations =
                jdbc.queryForList(
                        "SELECT feature_id,feature_context_id AS group_id FROM"
                                + " platform_feature_context_relations");
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target("81")
                .load()
                .migrate();
        assertThat(
                        jdbc.queryForList(
                                "SELECT * FROM service_context_microservices WHERE"
                                        + " identifier='rename-service'"))
                .isEqualTo(services);
        assertThat(
                        jdbc.queryForList(
                                "SELECT * FROM service_context_features WHERE"
                                        + " identifier='rename-feature'"))
                .isEqualTo(features);
        assertThat(
                        jdbc.queryForList(
                                "SELECT * FROM feature_group WHERE"
                                        + " identifier='rename-group'"))
                .isEqualTo(groups);
        assertThat(
                        jdbc.queryForList(
                                "SELECT feature_id,feature_group_id AS group_id FROM feature_group_feature"))
                .isEqualTo(associations);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM vw_service_context_feature_runtime_config"
                                        + " WHERE feature_code='rename-feature'",
                                Integer.class))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM schema_configuration WHERE"
                                    + " resource_type='SERVICE_CONTEXT' AND resource_code='feature-group'",
                                Integer.class))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()"
                                        + " AND table_name IN ('service_context_groups',"
                                        + " 'service_context_feature_groups')",
                                Integer.class))
                .isZero();
    }
}
