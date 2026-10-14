package br.com.portalmanager.platform.workspace.core.publisher.repository;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.support.MigrationSchemaValidationTestConfiguration;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@PlatformIntegrationTest
@WithMySql
@Import(MigrationSchemaValidationTestConfiguration.class)
@TestPropertySource(
    properties = {
        "spring.flyway.target=49",
        "platform.schema-validation.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
    }
)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PublisherMigrationIT {

    @Autowired
    private DataSource dataSource;

    @Test
    void upgradesExistingPublisherWithoutChangingItsIdentityOrSettings() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update(
            "INSERT IGNORE INTO type_life_cycle (code,label,sort_order,is_active,settings) VALUES ('ACTIVE','Active',1,true,'{}')"
        );
        jdbc.update(
            "INSERT IGNORE INTO type_resource_scopes (code,label,sort_order,is_active,settings) VALUES ('WORKSPACE','Workspace',1,true,'{}')"
        );
        jdbc.update(
            "INSERT IGNORE INTO type_schema_scopes (code,label,sort_order,is_active,settings) VALUES ('PLATFORM','Platform',1,true,'{}')"
        );
        jdbc.update(
            "INSERT IGNORE INTO type_schema_version_status (code,label,sort_order,is_active,settings) VALUES ('PUBLISHED','Published',1,true,'{}')"
        );
        jdbc.update(
            "INSERT INTO publishers (version,identifier,code,name,description,publisher_scope,deprecated,settings,lifecycle_code,created_at,updated_at) VALUES (7,UUID(),'KAFKA','Publisher','Existing publisher','WORKSPACE',false,'{\"enabled\":true}','ACTIVE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)"
        );
        String identifier = jdbc.queryForObject("select identifier from publishers where code='KAFKA'", String.class);
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        assertThat(
            jdbc.queryForObject("select identifier from publishers where code='KAFKA'", String.class)
        ).isEqualTo(identifier);
        assertThat(jdbc.queryForObject("select version from publishers where code='KAFKA'", Long.class)).isEqualTo(7L);
        assertThat(
            jdbc.queryForObject(
                "select JSON_EXTRACT(settings,'$.enabled') from publishers where code='KAFKA'",
                String.class
            )
        ).isEqualTo("true");
        assertThat(
            jdbc.queryForObject(
                "select CHARACTER_MAXIMUM_LENGTH from information_schema.columns where table_schema=database() and table_name='publishers' and column_name='description'",
                Long.class
            )
        ).isEqualTo(200L);
        assertThat(
            jdbc.queryForObject(
                "select count(*) from schema_configuration where resource_type='PUBLISHER' and resource_code='publisher'",
                Long.class
            )
        ).isEqualTo(1L);
        tools.jackson.databind.ObjectMapper mapper = tools.jackson.databind.json.JsonMapper.builder().build();
        String publishedSchema = jdbc.queryForObject(
            """
            SELECT v.definition FROM schema_versions v
            JOIN schema_definitions d ON d.id = v.schema_id
            WHERE d.scope_code = 'PLATFORM' AND d.code = 'publisher-resource'
              AND v.schema_version = 2 AND v.status = 'PUBLISHED'
            """,
            String.class
        );
        String referenceSchema = new org.springframework.core.io.ClassPathResource("schemas/publisher.json")
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(mapper.readTree(publishedSchema)).isEqualTo(mapper.readTree(referenceSchema));
    }
}
