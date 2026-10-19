package br.com.portalmanager.platform.workspace.feature.parameter.repository;

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
            "spring.flyway.target=67",
            "platform.schema-validation.enabled=false",
            "spring.jpa.hibernate.ddl-auto=none"
        })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ParameterKeyMigrationIT {
    @Autowired private DataSource dataSource;
    @Autowired private Flyway flyway;

    @Test
    void upgradesPreservingTheExistingGlobalMicroserviceAndEnforcesConfigurationConstraints()
            throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("67");
        jdbc.update(
                "INSERT IGNORE INTO type_life_cycle(code,label,sort_order,is_active,settings)"
                        + " VALUES ('ACTIVE','Active',1,true,'{}')");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM type_life_cycle WHERE code='INACTIVE'",
                                Integer.class))
                .isZero();
        jdbc.update(
                "INSERT IGNORE INTO type_schema_scopes(code,label,sort_order,is_active,settings)"
                        + " VALUES ('PLATFORM','Platform',1,true,'{}')");
        jdbc.update(
                "INSERT IGNORE INTO"
                    + " type_schema_version_status(code,label,sort_order,is_active,settings) VALUES"
                    + " ('PUBLISHED','Published',1,true,'{}')");
        jdbc.update(
                "INSERT INTO"
                    + " platform_microservices(version,identifier,code,name,settings,lifecycle_code,created_at,updated_at)"
                    + " VALUES (9,UUID(),'platform','Existing"
                    + " global','{\"custom\":true}','ACTIVE',NOW(),NOW())");
        Map<String, Object> before = jdbc.queryForMap("SELECT * FROM platform_microservices");
        Flyway upgrade =
                Flyway.configure()
                        .dataSource(dataSource)
                        .locations("classpath:db/migration")
                        .target("71")
                        .load();
        upgrade.migrate();
        upgrade.validate();
        Map<String, Object> previousContract =
                jdbc.queryForMap(
                        "SELECT sv.* FROM schema_versions sv JOIN schema_definitions sd ON"
                                + " sd.id=sv.schema_id WHERE sd.code='configuration-resource' AND"
                                + " sv.schema_version=2");
        Flyway latest =
                Flyway.configure()
                        .dataSource(dataSource)
                        .locations("classpath:db/migration")
                        .target("74")
                        .load();
        latest.migrate();
        latest.validate();
        assertThat(
                        jdbc.queryForMap(
                                "SELECT sv.* FROM schema_versions sv JOIN schema_definitions sd ON"
                                    + " sd.id=sv.schema_id WHERE sd.code='configuration-resource'"
                                    + " AND sv.schema_version=2"))
                .isEqualTo(previousContract);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM type_life_cycle WHERE code='INACTIVE'",
                                Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForMap("SELECT * FROM platform_microservices")).isEqualTo(before);
        JsonMapper mapper = JsonMapper.builder().build();
        for (Map.Entry<String, String> resource :
                Map.of(
                                "configuration",
                                "configuration",
                                "configuration-publication",
                                "configuration-publication",
                                "configuration-type",
                                "configuration-type-settings",
                                "configuration-status-type",
                                "configuration-status-type-settings")
                        .entrySet()) {
            String published =
                    jdbc.queryForObject(
                            "SELECT definition FROM vw_platform_resource_schemas WHERE"
                                    + " resource_code=?",
                            String.class,
                            resource.getKey());
            String reference =
                    new ClassPathResource("migration/" + resource.getValue() + ".json")
                            .getContentAsString(StandardCharsets.UTF_8);
            assertThat(mapper.readTree(published)).isEqualTo(mapper.readTree(reference));
        }
        assertThat(
                        jdbc.queryForObject(
                                "SELECT schema_version FROM vw_platform_resource_schemas WHERE"
                                    + " resource_type='PLATFORM' AND resource_code='configuration'",
                                Integer.class))
                .isEqualTo(4);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM vw_platform_resource_schemas WHERE"
                                        + " resource_code='configuration-expression'",
                                Integer.class))
                .isZero();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM schema_versions sv JOIN schema_definitions sd"
                                    + " ON sd.id=sv.schema_id WHERE"
                                    + " sd.code='configuration-resource' AND sv.status='PUBLISHED'",
                                Integer.class))
                .isEqualTo(4);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM schema_versions sv JOIN schema_definitions sd"
                                        + " ON sd.id=sv.schema_id WHERE"
                                        + " sd.code='configuration-expression' AND"
                                        + " sv.status='PUBLISHED'",
                                Integer.class))
                .isEqualTo(1);
        Long parent = ((Number) before.get("id")).longValue();
        jdbc.update(
                "INSERT INTO"
                    + " configurations(identifier,microservice_id,configuration_key,type_code,value,status_code,created_at,updated_at)"
                    + " VALUES"
                    + " ('11111111-1111-1111-1111-111111111111',?,'retry','CONFIGURATION','null','DRAFT',NOW(),NOW())",
                parent);
        assertThat(jdbc.queryForObject("SELECT value IS NULL FROM configurations", Boolean.class))
                .isFalse();
        assertThat(jdbc.queryForObject("SELECT JSON_TYPE(value) FROM configurations", String.class))
                .isEqualTo("NULL");
        assertThat(jdbc.queryForObject("SELECT active FROM configurations", Boolean.class))
                .isTrue();
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "INSERT INTO"
                                            + " configurations(identifier,microservice_id,configuration_key,type_code,value,status_code,created_at,updated_at)"
                                            + " VALUES"
                                            + " (UUID(),?,'retry','CONFIGURATION','{}','DRAFT',NOW(),NOW())",
                                        parent))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "INSERT INTO"
                                            + " configurations(identifier,microservice_id,configuration_key,type_code,value,status_code,created_at,updated_at)"
                                            + " VALUES"
                                            + " ('11111111-1111-1111-1111-111111111111',?,'other','CONFIGURATION','{}','DRAFT',NOW(),NOW())",
                                        parent))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE configurations SET microservice_id=999999"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE configurations SET type_code='INVALID'"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE configurations SET value=NULL"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE configurations SET status_code='PUBLISHED'"))
                .rootCause()
                .isInstanceOf(java.sql.SQLException.class)
                .extracting("errorCode")
                .isEqualTo(3819);
        assertThatThrownBy(() -> jdbc.update("UPDATE configurations SET published_value='null'"))
                .rootCause()
                .isInstanceOf(java.sql.SQLException.class)
                .extracting("errorCode")
                .isEqualTo(3819);
        for (String invalid :
                List.of(
                        "Retry",
                        "retryAttempts",
                        "RETRY",
                        "rétry",
                        "contains spaces",
                        "config:key",
                        "-leading",
                        "trailing.",
                        "double..dot")) {
            assertThatThrownBy(
                            () ->
                                    jdbc.update(
                                            "UPDATE configurations SET configuration_key=?",
                                            invalid))
                    .rootCause()
                    .isInstanceOf(java.sql.SQLException.class)
                    .extracting("errorCode")
                    .isEqualTo(3819);
        }
        jdbc.update(
                "UPDATE configurations SET"
                    + " published_value='null',published_type_code='CONFIGURATION',status_code='PUBLISHED'");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT published_value IS NULL FROM configurations",
                                Boolean.class))
                .isFalse();
        assertThatThrownBy(() -> jdbc.update("UPDATE configurations SET active=false"))
                .rootCause()
                .isInstanceOf(java.sql.SQLException.class)
                .extracting("errorCode")
                .isEqualTo(3819);
        jdbc.update(
                "UPDATE configurations SET"
                    + " active=false,status_code='DRAFT',published_value=NULL,published_type_code=NULL");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT published_value IS NULL FROM configurations",
                                Boolean.class))
                .isTrue();
        assertThatThrownBy(
                        () -> jdbc.update("DELETE FROM platform_microservices WHERE id=?", parent))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT DELETE_RULE FROM information_schema.REFERENTIAL_CONSTRAINTS"
                                        + " WHERE CONSTRAINT_SCHEMA=DATABASE() AND"
                                        + " CONSTRAINT_NAME='fk_configuration_microservice'",
                                String.class))
                .isIn("RESTRICT", "NO ACTION");
        Map<String, Object> parameterBefore = jdbc.queryForMap("SELECT * FROM configurations");
        Object configurationKey = parameterBefore.remove("configuration_key");
        Flyway renamed =
                Flyway.configure()
                        .dataSource(dataSource)
                        .locations("classpath:db/migration")
                        .target("76")
                        .load();
        renamed.migrate();
        renamed.validate();
        Map<String, Object> parameterAfter = jdbc.queryForMap("SELECT * FROM parameter_keys");
        assertThat(parameterAfter.remove("parameter_key")).isEqualTo(configurationKey);
        assertThat(parameterAfter).isEqualTo(parameterBefore);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM information_schema.TABLES WHERE"
                                    + " TABLE_SCHEMA=DATABASE() AND TABLE_NAME='configurations'",
                                Integer.class))
                .isZero();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT schema_version FROM vw_platform_resource_schemas WHERE"
                                        + " resource_type='PLATFORM' AND"
                                        + " resource_code='dynamic-parameter'",
                                Integer.class))
                .isEqualTo(5);
        assertThat(
                        jdbc.queryForMap(
                                "SELECT sv.* FROM schema_versions sv JOIN schema_definitions sd ON"
                                        + " sd.id=sv.schema_id WHERE"
                                        + " sd.code='dynamic-parameter-resource' AND"
                                        + " sv.schema_version=2"))
                .isEqualTo(previousContract);
        assertThatThrownBy(() -> jdbc.update("UPDATE parameter_keys SET parameter_key='UPPERCASE'"))
                .rootCause()
                .isInstanceOf(java.sql.SQLException.class)
                .extracting("errorCode")
                .isEqualTo(3819);
        assertThatThrownBy(() -> jdbc.update("UPDATE parameter_keys SET status_code='PUBLISHED'"))
                .rootCause()
                .isInstanceOf(java.sql.SQLException.class)
                .extracting("errorCode")
                .isEqualTo(3819);
    }

    @Test
    void nativeJsonRoundsDecimalsWhileAValidatedTextDocumentPreservesThem() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute(
                (org.springframework.jdbc.core.ConnectionCallback<Void>)
                        connection -> {
                            try (java.sql.Statement statement = connection.createStatement()) {
                                statement.execute(
                                        "CREATE TEMPORARY TABLE"
                                            + " configuration_precision_probe(native_value JSON NOT"
                                            + " NULL, precise_value LONGTEXT NOT NULL, CHECK"
                                            + " (JSON_VALID(precise_value)))");
                                try {
                                    tools.jackson.databind.json.JsonMapper mapper =
                                            tools.jackson.databind.json.JsonMapper.builder()
                                                    .enable(
                                                            tools.jackson.databind
                                                                    .DeserializationFeature
                                                                    .USE_BIG_DECIMAL_FOR_FLOATS)
                                                    .build();
                                    for (String value :
                                            List.of(
                                                    "19.900000000000000000001",
                                                    "[19.900000000000000000001]",
                                                    "{\"amount\":19.900000000000000000001}")) {
                                        statement.execute(
                                                "DELETE FROM configuration_precision_probe");
                                        try (java.sql.PreparedStatement insert =
                                                connection.prepareStatement(
                                                        "INSERT INTO configuration_precision_probe"
                                                                + " VALUES (?,?)")) {
                                            insert.setString(1, value);
                                            insert.setString(2, value);
                                            insert.executeUpdate();
                                        }
                                        try (java.sql.ResultSet result =
                                                statement.executeQuery(
                                                        "SELECT native_value,precise_value FROM"
                                                            + " configuration_precision_probe")) {
                                            assertThat(result.next()).isTrue();
                                            assertThat(mapper.readTree(result.getString(1)))
                                                    .isNotEqualTo(mapper.readTree(value));
                                            assertThat(mapper.readTree(result.getString(2)))
                                                    .isEqualTo(mapper.readTree(value));
                                        }
                                    }
                                } finally {
                                    statement.execute(
                                            "DROP TEMPORARY TABLE configuration_precision_probe");
                                }
                            }
                            return null;
                        });
    }
}
