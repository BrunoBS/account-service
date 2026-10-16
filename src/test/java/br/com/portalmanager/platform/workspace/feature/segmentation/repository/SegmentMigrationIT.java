package br.com.portalmanager.platform.workspace.feature.segmentation.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.support.MigrationSchemaValidationTestConfiguration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

@PlatformIntegrationTest
@WithMySql
@Import(MigrationSchemaValidationTestConfiguration.class)
@TestPropertySource(
        properties = {
            "spring.flyway.target=65",
            "platform.schema-validation.enabled=false",
            "spring.jpa.hibernate.ddl-auto=none"
        })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SegmentMigrationIT {
    @Autowired private DataSource dataSource;
    @Autowired private Flyway flyway;

    @Test
    void upgradesTheExistingDatabaseAndPublishesTheExactReferenceContracts() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("65");
        jdbc.update(
                "INSERT IGNORE INTO type_life_cycle(code,label,sort_order,is_active,settings)"
                    + " VALUES"
                    + " ('ACTIVE','Active',1,true,'{}'),('INACTIVE','Inactive',2,true,'{}'),('QUARANTINED','Quarantined',3,true,'{}')");
        jdbc.update(
                "INSERT IGNORE INTO type_schema_scopes(code,label,sort_order,is_active,settings)"
                        + " VALUES ('PLATFORM','Platform',1,true,'{}')");
        jdbc.update(
                "INSERT IGNORE INTO"
                    + " type_schema_version_status(code,label,sort_order,is_active,settings) VALUES"
                    + " ('PUBLISHED','Published',1,true,'{}')");
        jdbc.update(
                "INSERT INTO"
                    + " platform_feature_contexts(version,identifier,code,name,lifecycle_code,created_at,updated_at)"
                    + " VALUES (9,UUID(),'existing-context','Existing"
                    + " context','INACTIVE',NOW(6),NOW(6))");
        Map<String, Object> before = jdbc.queryForMap("SELECT * FROM platform_feature_contexts");
        Flyway upgrade =
                Flyway.configure()
                        .dataSource(dataSource)
                        .locations("classpath:db/migration")
                        .target("67")
                        .load();
        upgrade.migrate();
        upgrade.validate();
        assertThat(upgrade.info().current().getVersion().getVersion()).isEqualTo("67");
        assertThat(jdbc.queryForMap("SELECT * FROM platform_feature_contexts")).isEqualTo(before);
        JsonMapper mapper = JsonMapper.builder().build();
        for (String resource : List.of("segment", "segment-category")) {
            String published =
                    jdbc.queryForObject(
                            "SELECT definition FROM vw_platform_resource_schemas WHERE"
                                    + " resource_type='PLATFORM' AND resource_code=?",
                            String.class,
                            resource);
            String reference =
                    new ClassPathResource(
                                    (resource.equals("segment-category")
                                                    ? "migration/segmentation/"
                                                    : "schemas/")
                                            + resource
                                            + ".json")
                            .getContentAsString(StandardCharsets.UTF_8);
            assertThat(mapper.readTree(published)).isEqualTo(mapper.readTree(reference));
        }
        jdbc.update(
                "INSERT INTO segment(identifier,code,name,lifecycle_code,created_at,updated_at)"
                    + " VALUES"
                    + " ('11111111-1111-1111-1111-111111111111','mobile-pf','Mobile','ACTIVE',NOW(6),NOW(6))");
        Long segment = jdbc.queryForObject("SELECT id FROM segment", Long.class);
        jdbc.update(
                "INSERT INTO"
                    + " segment_category(identifier,segment_id,code,name,lifecycle_code,created_at,updated_at)"
                    + " VALUES"
                    + " ('22222222-2222-2222-2222-222222222222',?,'premium','Premium','INACTIVE',NOW(6),NOW(6))",
                segment);
        // Constraints protect codes and UUIDs even when writes bypass the application.
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "INSERT INTO"
                                            + " segment(identifier,code,name,lifecycle_code,created_at,updated_at)"
                                            + " VALUES"
                                            + " (UUID(),'mobile-pf','Duplicate','INACTIVE',NOW(6),NOW(6))"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "INSERT INTO"
                                            + " segment(identifier,code,name,lifecycle_code,created_at,updated_at)"
                                            + " VALUES"
                                            + " ('11111111-1111-1111-1111-111111111111','other','Duplicate"
                                            + " UUID','ACTIVE',NOW(6),NOW(6))"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "INSERT INTO"
                                            + " segment_category(identifier,segment_id,code,name,lifecycle_code,created_at,updated_at)"
                                            + " VALUES"
                                            + " (UUID(),?,'premium','Duplicate','ACTIVE',NOW(6),NOW(6))",
                                        segment))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "INSERT INTO"
                                            + " segment_category(identifier,segment_id,code,name,lifecycle_code,created_at,updated_at)"
                                            + " VALUES"
                                            + " ('22222222-2222-2222-2222-222222222222',?,'basic','Duplicate"
                                            + " UUID','ACTIVE',NOW(6),NOW(6))",
                                        segment))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE segment_category SET segment_id=999999"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM segment WHERE id=?", segment))
                .isInstanceOf(DataIntegrityViolationException.class);
        for (String code : List.of("UPPER", "a.b", "a_b", "a--b", "-a")) {
            assertThatThrownBy(
                            () ->
                                    jdbc.update(
                                            "UPDATE segment SET code=? WHERE id=?", code, segment))
                    .rootCause()
                    .isInstanceOf(java.sql.SQLException.class)
                    .extracting("errorCode")
                    .isEqualTo(3819);
        }
        assertThatThrownBy(() -> jdbc.update("UPDATE segment SET lifecycle_code='QUARANTINED'"))
                .rootCause()
                .isInstanceOf(java.sql.SQLException.class)
                .extracting("errorCode")
                .isEqualTo(3819);
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "UPDATE segment_category SET lifecycle_code='QUARANTINED'"))
                .rootCause()
                .isInstanceOf(java.sql.SQLException.class)
                .extracting("errorCode")
                .isEqualTo(3819);
        assertThatThrownBy(() -> jdbc.update("UPDATE segment SET name='   '"))
                .rootCause()
                .isInstanceOf(java.sql.SQLException.class)
                .extracting("errorCode")
                .isEqualTo(3819);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT DELETE_RULE FROM information_schema.REFERENTIAL_CONSTRAINTS"
                                        + " WHERE CONSTRAINT_SCHEMA=DATABASE() AND"
                                        + " CONSTRAINT_NAME='fk_segment_category_segment'",
                                String.class))
                .isIn("NO ACTION", "RESTRICT");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM segment_category", Integer.class))
                .isEqualTo(1);
    }
}
