package br.com.portalmanager.platform.workspace.feature.servicecontext.repository.microservice;

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
            "spring.flyway.target=61",
            "platform.schema-validation.enabled=false",
            "spring.jpa.hibernate.ddl-auto=none"
        })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MicroserviceResourceSchemaMigrationIT {

    @Autowired private DataSource dataSource;
    @Autowired private Flyway flyway;

    @Test
    void publishesTheContractWithoutChangingExistingMicroserviceData() throws Exception {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("61");
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
        Map<String, Object> before = jdbc.queryForMap("SELECT * FROM platform_microservices");
        Flyway upgrade =
                Flyway.configure()
                        .dataSource(dataSource)
                        .locations("classpath:db/migration")
                        .target("62")
                        .load();
        upgrade.migrate();
        assertThat(upgrade.info().current().getVersion().getVersion()).isEqualTo("62");
        String published =
                jdbc.queryForObject(
                        "SELECT sv.definition FROM schema_versions sv JOIN schema_definitions sd ON"
                                + " sd.id=sv.schema_id JOIN schema_configuration sc ON"
                                + " sc.schema_id=sd.id WHERE sc.resource_type='PLATFORM' AND"
                                + " sc.resource_code='microservice' AND sv.status='PUBLISHED'",
                        String.class);
        String reference =
                new ClassPathResource("schemas/microservice.json")
                        .getContentAsString(StandardCharsets.UTF_8);
        JsonMapper mapper = JsonMapper.builder().build();
        assertThat(mapper.readTree(published)).isEqualTo(mapper.readTree(reference));
        assertThat(jdbc.queryForMap("SELECT * FROM platform_microservices")).isEqualTo(before);
    }
}
