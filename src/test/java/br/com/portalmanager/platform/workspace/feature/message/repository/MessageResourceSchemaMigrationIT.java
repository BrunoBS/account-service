package br.com.portalmanager.platform.workspace.feature.message.repository;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.support.MigrationSchemaValidationTestConfiguration;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
import tools.jackson.databind.json.JsonMapper;

@PlatformIntegrationTest
@WithMySql
@Import(MigrationSchemaValidationTestConfiguration.class)
@TestPropertySource(properties = {"spring.flyway.target=57", "platform.schema-validation.enabled=false", "spring.jpa.hibernate.ddl-auto=none"})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MessageResourceSchemaMigrationIT {

    @Autowired
    private DataSource dataSource;
    @Autowired
    private Flyway flyway;

    @Test
    void publishesContractsMatchingReferenceFilesWithoutChangingExistingMessages() throws Exception {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("57");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT IGNORE INTO type_life_cycle (code,label,sort_order,is_active,settings) VALUES ('ACTIVE','Active',1,true,'{}')");
        jdbc.update("INSERT IGNORE INTO type_schema_scopes (code,label,sort_order,is_active,settings) VALUES ('PLATFORM','Platform',1,true,'{}')");
        jdbc.update("INSERT IGNORE INTO type_schema_version_status (code,label,sort_order,is_active,settings) VALUES ('PUBLISHED','Published',1,true,'{}')");
        jdbc.update("INSERT INTO platform_microservices (identifier,code,name,settings,lifecycle_code,created_at,updated_at) VALUES ('11111111-1111-1111-1111-111111111111','review-service','Review service','{}','ACTIVE',NOW(6),NOW(6))");
        jdbc.update("INSERT INTO messages (identifier,microservice_id,message_key,code,http_status,lifecycle_code,created_at,updated_at) SELECT '22222222-2222-2222-2222-222222222222',id,'review.migration','REVIEW-101',400,'ACTIVE',NOW(6),NOW(6) FROM platform_microservices");
        Flyway upgrade = Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("58").load();
        upgrade.migrate();
        assertThat(upgrade.info().current().getVersion().getVersion()).isEqualTo("58");
        JsonMapper mapper = JsonMapper.builder().build();
        for (String resource : List.of("message", "message-translation")) {
            String published = jdbc.queryForObject("SELECT sv.definition FROM schema_versions sv JOIN schema_definitions sd ON sd.id=sv.schema_id JOIN schema_configuration sc ON sc.schema_id=sd.id WHERE sc.resource_type='MESSAGE' AND sc.resource_code=? AND sv.status='PUBLISHED'", String.class, resource);
            String reference = new ClassPathResource("schemas/" + resource + ".json").getContentAsString(StandardCharsets.UTF_8);
            assertThat(mapper.readTree(published)).isEqualTo(mapper.readTree(reference));
        }
        assertThat(jdbc.queryForObject("SELECT message_key FROM messages WHERE identifier='22222222-2222-2222-2222-222222222222'", String.class)).isEqualTo("review.migration");
    }
}
