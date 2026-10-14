package br.com.portalmanager.platform.workspace.feature.onboarding.repository;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.support.MigrationSchemaValidationTestConfiguration;
import java.nio.charset.StandardCharsets;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@PlatformIntegrationTest
@WithMySql
@Import(MigrationSchemaValidationTestConfiguration.class)
@TestPropertySource(properties = {
    "spring.flyway.target=60", "platform.schema-validation.enabled=false", "spring.jpa.hibernate.ddl-auto=none"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OnboardingPhaseSettingsSchemaMigrationIT {

    @Autowired
    private DataSource dataSource;

    @Test
    void publishesTheCatalogSettingsContractMatchingTheReferenceSchema() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT IGNORE INTO type_life_cycle (code,label,sort_order,is_active,settings) VALUES ('ACTIVE','Active',1,true,'{}')");
        jdbc.update("INSERT IGNORE INTO type_schema_scopes (code,label,sort_order,is_active,settings) VALUES ('PLATFORM','Platform',1,true,'{}')");
        jdbc.update("INSERT IGNORE INTO type_schema_version_status (code,label,sort_order,is_active,settings) VALUES ('PUBLISHED','Published',1,true,'{}')");
        Flyway upgrade = Flyway.configure().dataSource(dataSource)
            .locations("classpath:db/migration").target("61").load();
        upgrade.migrate();

        JsonMapper json = JsonMapper.builder().build();
        JsonNode published = json.readTree(jdbc.queryForObject("""
            SELECT v.definition FROM schema_configuration c
            JOIN schema_definitions s ON s.id = c.schema_id
            JOIN schema_versions v ON v.schema_id = s.id
            WHERE c.resource_type = 'CATALOG' AND c.resource_code = 'onboarding-phase-type'
              AND c.lifecycle_code = 'ACTIVE' AND s.lifecycle_code = 'ACTIVE' AND v.status = 'PUBLISHED'
            """, String.class));
        JsonNode reference = json.readTree(new ClassPathResource("schemas/onboarding-phase-type-settings.json")
            .getContentAsString(StandardCharsets.UTF_8));
        assertThat(upgrade.info().current().getVersion().getVersion()).isEqualTo("61");
        assertThat(published).isEqualTo(reference);
    }
}
