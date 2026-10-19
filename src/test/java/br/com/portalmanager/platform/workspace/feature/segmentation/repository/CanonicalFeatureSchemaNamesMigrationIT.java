package br.com.portalmanager.platform.workspace.feature.segmentation.repository;

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
            "spring.flyway.target=81",
            "platform.schema-validation.enabled=false",
            "spring.jpa.hibernate.ddl-auto=none"
        })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CanonicalFeatureSchemaNamesMigrationIT {
    @Autowired private DataSource dataSource;

    @Test
    void publishesCanonicalFeatureGroupAndCategorySchemaNamesWithoutAlteringPayloads() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        seedCatalogs(jdbc);
        seedSchema(
                jdbc,
                "feature-context-resource",
                "Feature Context Resource",
                "PLATFORM",
                "feature-context",
                "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"title\":\"Feature context associations request\",\"type\":\"object\",\"properties\":{\"featureIdentifiers\":{\"type\":\"array\"}}}");
        seedSchema(
                jdbc,
                "category-resource",
                "Category request",
                "SEGMENTATION",
                "category",
                "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"title\":\"Segment Category request\",\"type\":\"object\",\"required\":[\"code\",\"name\"]}");
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target("82")
                .load()
                .migrate();

        assertThat(schemaTitle(jdbc, "SERVICE_CONTEXT", "feature-group-feature"))
                .isEqualTo("Feature Group associations request");
        assertThat(schemaTitle(jdbc, "SEGMENTATION", "category"))
                .isEqualTo("Category request");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT JSON_UNQUOTE(JSON_EXTRACT(sv.definition, '$.properties.featureIdentifiers.type'))"
                                        + " FROM schema_configuration sc JOIN schema_versions sv"
                                        + " ON sv.schema_id=sc.schema_id AND sv.status='PUBLISHED'"
                                        + " WHERE sc.resource_type='SERVICE_CONTEXT'"
                                        + " AND sc.resource_code='feature-group-feature'"
                                        + " ORDER BY sv.schema_version DESC LIMIT 1",
                                String.class))
                .isEqualTo("array");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM schema_definitions WHERE scope_code='PLATFORM'"
                                        + " AND code IN ('feature-context-resource', 'segment-category-resource')",
                                Integer.class))
                .isZero();
    }

    private void seedCatalogs(JdbcTemplate jdbc) {
        jdbc.update(
                "INSERT IGNORE INTO type_life_cycle(code,label,sort_order,is_active,settings)"
                        + " VALUES ('ACTIVE','Active',1,true,'{}')");
        jdbc.update(
                "INSERT IGNORE INTO type_schema_scopes(code,label,sort_order,is_active,settings)"
                        + " VALUES ('PLATFORM','Platform',1,true,'{}')");
        jdbc.update(
                "INSERT IGNORE INTO type_schema_version_status"
                        + " (code,label,sort_order,is_active,settings)"
                        + " VALUES ('PUBLISHED','Published',1,true,'{}')");
    }

    private void seedSchema(
            JdbcTemplate jdbc,
            String definitionCode,
            String definitionName,
            String resourceType,
            String resourceCode,
            String definition) {
        jdbc.update(
                "INSERT INTO schema_definitions"
                        + " (version,identifier,scope_code,code,name,lifecycle_code,created_at,updated_at)"
                        + " VALUES (0,UUID(),'PLATFORM',?,?, 'ACTIVE',NOW(6),NOW(6))",
                definitionCode,
                definitionName);
        Long schemaId =
                jdbc.queryForObject(
                        "SELECT id FROM schema_definitions WHERE scope_code='PLATFORM' AND code=?",
                        Long.class,
                        definitionCode);
        jdbc.update(
                "INSERT INTO schema_versions"
                        + " (identifier,schema_id,schema_version,version_name,definition,status,created_at)"
                        + " VALUES (UUID(),?,1,?,?,'PUBLISHED',NOW(6))",
                schemaId,
                definitionName + " v1",
                definition);
        jdbc.update(
                "INSERT INTO schema_configuration"
                        + " (version,identifier,resource_type,resource_code,schema_id,lifecycle_code,created_at,updated_at)"
                        + " VALUES (0,UUID(),?,?,?,'ACTIVE',NOW(6),NOW(6))",
                resourceType,
                resourceCode,
                schemaId);
    }

    private String schemaTitle(JdbcTemplate jdbc, String resourceType, String resourceCode) {
        return jdbc.queryForObject(
                "SELECT JSON_UNQUOTE(JSON_EXTRACT(sv.definition, '$.title'))"
                        + " FROM schema_configuration sc JOIN schema_versions sv"
                        + " ON sv.schema_id=sc.schema_id AND sv.status='PUBLISHED'"
                        + " WHERE sc.resource_type=? AND sc.resource_code=?"
                        + " ORDER BY sv.schema_version DESC LIMIT 1",
                String.class,
                resourceType,
                resourceCode);
    }
}
