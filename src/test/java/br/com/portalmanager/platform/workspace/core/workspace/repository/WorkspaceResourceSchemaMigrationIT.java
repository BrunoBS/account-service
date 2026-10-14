package br.com.portalmanager.platform.workspace.core.workspace.repository;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.support.MigrationSchemaValidationTestConfiguration;
import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@PlatformIntegrationTest
@WithMySql
@Import(MigrationSchemaValidationTestConfiguration.class)
@TestPropertySource(
    properties = {
        "spring.flyway.target=41",
        "platform.schema-validation.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
    }
)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class WorkspaceResourceSchemaMigrationIT {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway flyway;

    @Test
    void shouldPublishOneWorkspaceSchemaMatchingTheResourceExample() throws java.io.IOException {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("41");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update(
            "INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings) VALUES ('ACTIVE', 'Active', 'Active lifecycle', 1, true, '{}')"
        );
        jdbcTemplate.update(
            "INSERT IGNORE INTO type_schema_scopes (code, label, description, sort_order, is_active, settings) VALUES ('PLATFORM', 'Platform', 'Platform scope', 1, true, '{}')"
        );
        jdbcTemplate.update(
            "INSERT IGNORE INTO type_schema_version_status (code, label, description, sort_order, is_active, settings) VALUES ('PUBLISHED', 'Published', 'Published schema version', 1, true, '{}')"
        );
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        for (String[] column : List.of(
            new String[] { "workspaces", "name", "50" },
            new String[] { "workspaces", "description", "200" },
            new String[] { "workspaces", "requester", "10" },
            new String[] { "workspaces", "authorizer_group", "10" },
            new String[] { "workspaces", "email_group", "100" },
            new String[] { "workspaces", "workspace_type_code", "20" },
            new String[] { "workspaces", "lifecycle_code", "20" },
            new String[] { "workspace_approvers", "functional", "10" },
            new String[] { "workspace_approvers", "email", "100" },
            new String[] { "workspace_tag", "name", "50" }
        )) {
            assertThat(
                jdbcTemplate.queryForObject(
                    "select CHARACTER_MAXIMUM_LENGTH from information_schema.COLUMNS where TABLE_SCHEMA = database() and TABLE_NAME = ? and COLUMN_NAME = ?",
                    Long.class,
                    column[0],
                    column[1]
                )
            ).isEqualTo(Long.valueOf(column[2]));
        }
        JsonMapper objectMapper = JsonMapper.builder().build();
        List<JsonNode> definitions = jdbcTemplate.query(
            """
            select version.definition
              from schema_configuration configuration
              join schema_definitions definition on definition.id = configuration.schema_id
              join schema_versions version on version.schema_id = definition.id
             where configuration.resource_type = 'WORKSPACE'
               and configuration.resource_code = 'workspace'
               and configuration.lifecycle_code = 'ACTIVE'
               and definition.lifecycle_code = 'ACTIVE'
               and version.status = 'PUBLISHED'
               and version.schema_version = 2
            """,
            (resultSet, rowNumber) -> objectMapper.readTree(resultSet.getString("definition"))
        );
        assertThat(definitions).hasSize(1);
        JsonNode schema = definitions.getFirst();
        try (
            java.io.InputStream inputStream = new org.springframework.core.io.ClassPathResource(
                "schemas/workspace.json"
            ).getInputStream()
        ) {
            assertThat(schema).isEqualTo(objectMapper.readTree(inputStream));
        }
        assertThat(schema.get("required")).doesNotContain(objectMapper.valueToTree("version"));
        assertThat(schema.at("/properties/version/type")).contains(
            objectMapper.valueToTree("integer"),
            objectMapper.valueToTree("null")
        );
        assertThat(schema.at("/properties/version/minimum").asInt()).isZero();
        assertThat(schema.at("/properties/version/maximum").asLong()).isEqualTo(Long.MAX_VALUE);
    }
}
