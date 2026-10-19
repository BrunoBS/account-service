package br.com.portalmanager.platform.workspace.feature.servicecontext.repository.featuregroup;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.support.MigrationSchemaValidationTestConfiguration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.sql.DataSource;

@PlatformIntegrationTest
@WithMySql
@Import(MigrationSchemaValidationTestConfiguration.class)
@TestPropertySource(
        properties = {
            "spring.flyway.target=63",
            "platform.schema-validation.enabled=false",
            "spring.jpa.hibernate.ddl-auto=none"
        })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FeatureGroupResourceSchemaMigrationIT {

    @Autowired private DataSource dataSource;
    @Autowired private Flyway flyway;

    @Test
    void publishesTheContractWithoutChangingExistingContextsOrAssociations() throws Exception {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("63");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update(
                "INSERT IGNORE INTO type_life_cycle (code,label,sort_order,is_active,settings)"
                        + " VALUES ('ACTIVE','Active',1,true,'{}')");
        jdbc.update(
                "INSERT IGNORE INTO type_schema_scopes (code,label,sort_order,is_active,settings)"
                        + " VALUES ('PLATFORM','Platform',1,true,'{}')");
        jdbc.update(
                "INSERT IGNORE INTO type_schema_version_status"
                        + " (code,label,sort_order,is_active,settings) VALUES"
                        + " ('PUBLISHED','Published',1,true,'{}')");
        jdbc.update(
                "INSERT INTO platform_microservices"
                    + " (version,identifier,code,name,description,settings,lifecycle_code,created_at,updated_at)"
                    + " VALUES (7,'11111111-1111-1111-1111-111111111111','review-service',?,?,"
                    + " '{\"enabled\":true}','ACTIVE',NOW(6),NOW(6))",
                "n".repeat(100),
                "d".repeat(500));
        jdbc.update(
                "INSERT INTO platform_features"
                    + " (version,identifier,code,name,description,microservice_id,settings,shareable,lifecycle_code,created_at,updated_at)"
                    + " SELECT"
                    + " 5,UUID(),'review-feature',?,?,id,'{\"enabled\":true}',true,'ACTIVE',NOW(6),NOW(6)"
                    + " FROM platform_microservices",
                "f".repeat(100),
                "e".repeat(500));
        jdbc.update(
                "INSERT INTO platform_feature_contexts"
                        + " (identifier,code,name,lifecycle_code,created_at,updated_at) VALUES"
                        + " (UUID(),'review-context','Review context','ACTIVE',NOW(6),NOW(6))");
        jdbc.update(
                "INSERT INTO platform_feature_context_relations (feature_id,feature_context_id)"
                        + " SELECT feature.id,context.id FROM platform_features feature CROSS JOIN"
                        + " platform_feature_contexts context");
        jdbc.update(
                "UPDATE platform_feature_contexts SET version=8,name=?,description=?",
                "c".repeat(100),
                "d".repeat(500));
        Map<String, Object> before = jdbc.queryForMap("SELECT * FROM platform_feature_contexts");
        Map<String, Object> associationBefore =
                jdbc.queryForMap("SELECT * FROM platform_feature_context_relations");
        Flyway upgrade =
                Flyway.configure()
                        .dataSource(dataSource)
                        .locations("classpath:db/migration")
                        .target("65")
                        .load();
        upgrade.migrate();
        assertThat(upgrade.info().current().getVersion().getVersion()).isEqualTo("65");
        String published =
                jdbc.queryForObject(
                        "SELECT sv.definition FROM schema_versions sv JOIN schema_definitions sd ON"
                                + " sd.id=sv.schema_id JOIN schema_configuration sc ON"
                                + " sc.schema_id=sd.id WHERE sc.resource_type='PLATFORM' AND"
                                + " sc.resource_code='context' AND sv.status='PUBLISHED' ORDER BY"
                                + " sv.schema_version DESC LIMIT 1",
                        String.class);
        String reference =
                new ClassPathResource("migration/service-context/context.json")
                        .getContentAsString(StandardCharsets.UTF_8);
        JsonMapper mapper = JsonMapper.builder().build();
        assertThat(mapper.readTree(published)).isEqualTo(mapper.readTree(reference));
        String associationSchema =
                jdbc.queryForObject(
                        "SELECT sv.definition FROM schema_versions sv JOIN schema_definitions sd ON"
                                + " sd.id=sv.schema_id JOIN schema_configuration sc ON"
                                + " sc.schema_id=sd.id WHERE sc.resource_type='PLATFORM' AND"
                                + " sc.resource_code='feature-context' AND sv.status='PUBLISHED'",
                        String.class);
        String associationReference =
                new ClassPathResource("migration/service-context/feature-context.json")
                        .getContentAsString(StandardCharsets.UTF_8);
        assertThat(mapper.readTree(associationSchema))
                .isEqualTo(mapper.readTree(associationReference));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM schema_versions sv JOIN schema_definitions sd"
                                    + " ON sd.id=sv.schema_id WHERE sd.code='context-resource' AND"
                                    + " sv.schema_version=1 AND sv.status='PUBLISHED'",
                                Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForMap("SELECT * FROM platform_feature_contexts")).isEqualTo(before);
        assertThat(jdbc.queryForMap("SELECT * FROM platform_feature_context_relations"))
                .isEqualTo(associationBefore);
    }
}
