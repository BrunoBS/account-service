package br.com.portalmanager.platform.workspace.core.application.repository;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.support.MigrationSchemaValidationTestConfiguration;
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
@TestPropertySource(
    properties = {
        "spring.flyway.target=46",
        "platform.schema-validation.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
    }
)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ApplicationResourceSchemaMigrationIT {

    @Autowired
    private DataSource dataSource;

    @Test
    void upgradesExistingJsonSettingsAndPublishesTheReferenceContract() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update(
            "INSERT IGNORE INTO type_workspaces (code, label, sort_order, is_active, settings) VALUES ('MANAGER', 'Manager', 1, true, '{}')"
        );
        jdbc.update(
            "INSERT IGNORE INTO type_life_cycle (code, label, sort_order, is_active, settings) VALUES ('ACTIVE', 'Active', 1, true, '{}')"
        );
        jdbc.update(
            "INSERT IGNORE INTO type_application_scopes (code, label, sort_order, is_active, settings) VALUES ('BACKEND', 'Backend', 1, true, '{}')"
        );
        jdbc.update(
            """
            INSERT INTO workspaces (version, identifier, workspace_type_code, name, description, requester, acronym,
                settings, email_group, onboarding, lifecycle_code, created_at, updated_at)
            VALUES (0, UUID(), 'MANAGER', 'Migration Application Parent', 'Migration parent workspace', 'requester', 'APP',
                '{}', 'migration@example.com', false, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """
        );
        Long workspaceId = jdbc.queryForObject(
            "SELECT id FROM workspaces WHERE name = 'Migration Application Parent'",
            Long.class
        );
        jdbc.update(
            """
            INSERT INTO applications (version, identifier, workspace_id, name, alias, acronym, application_scope_code,
                settings, lifecycle_code, created_at, updated_at)
            VALUES (0, UUID(), ?, 'Migration Application', 'alias', 'APP', 'BACKEND', '{"enabled":true}',
                'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """,
            workspaceId
        );
        // Catalog rows/schema definition may be cleaned by the fixture; republish the original definition for upgrade.
        jdbc.update(
            "INSERT IGNORE INTO type_schema_scopes (code, label, sort_order, is_active, settings) VALUES ('PLATFORM', 'Platform', 1, true, '{}')"
        );
        jdbc.update(
            "INSERT IGNORE INTO type_schema_version_status (code, label, sort_order, is_active, settings) VALUES ('PUBLISHED', 'Published', 1, true, '{}')"
        );
        jdbc.update(
            """
            INSERT IGNORE INTO schema_definitions (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
            VALUES (0, UUID(), 'PLATFORM', 'application-resource', 'Application Resource', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """
        );
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        assertThat(
            jdbc.queryForObject(
                "SELECT JSON_EXTRACT(settings, '$.enabled') FROM applications WHERE workspace_id=?",
                String.class,
                workspaceId
            )
        ).isEqualTo("true");
        assertThat(
            jdbc.queryForObject(
                "SELECT DATA_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=database() AND TABLE_NAME='applications' AND COLUMN_NAME='settings'",
                String.class
            )
        ).isEqualTo("json");
        assertThat(
            jdbc.queryForObject(
                "SELECT COLLATION_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=database() AND TABLE_NAME='applications' AND COLUMN_NAME='name'",
                String.class
            )
        ).isEqualTo("utf8mb4_unicode_ci");
        for (String[] column : java.util.List.of(
            new String[] { "applications", "name", "50" },
            new String[] { "applications", "alias", "50" },
            new String[] { "applications", "description", "200" },
            new String[] { "workspaces", "name", "50" },
            new String[] { "workspaces", "authorizer_group", "10" },
            new String[] { "workspaces", "requester", "10" },
            new String[] { "workspaces", "description", "200" },
            new String[] { "workspaces", "email_group", "100" },
            new String[] { "workspace_approvers", "email", "100" },
            new String[] { "applications", "acronym", "5" },
            new String[] { "applications", "authorizer_group", "10" },
            new String[] { "applications", "application_scope_code", "20" },
            new String[] { "applications", "lifecycle_code", "20" },
            new String[] { "application_tag", "name", "50" }
        )) {
            assertThat(
                jdbc.queryForObject(
                    "SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=database() AND TABLE_NAME=? AND COLUMN_NAME=?",
                    Long.class,
                    column[0],
                    column[1]
                )
            ).isEqualTo(Long.valueOf(column[2]));
        }
        assertThat(
            jdbc.queryForObject(
                "SELECT IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=database() AND TABLE_NAME='applications' AND COLUMN_NAME='settings'",
                String.class
            )
        ).isEqualTo("NO");
        String definition = jdbc.queryForObject(
            """
            SELECT version.definition FROM schema_versions version JOIN schema_definitions schema_definition
                ON schema_definition.id=version.schema_id
            WHERE schema_definition.code='application-resource' AND version.schema_version=4 AND version.status='PUBLISHED'
            """,
            String.class
        );
        JsonMapper mapper = JsonMapper.builder().build();
        try (java.io.InputStream stream = new ClassPathResource("schemas/application.json").getInputStream()) {
            assertThat(mapper.readTree(definition)).isEqualTo(mapper.readTree(stream));
        }
    }
}
