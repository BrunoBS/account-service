package br.com.portalmanager.platform.workspace.core.environment.repository;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.support.MigrationSchemaValidationTestConfiguration;
import javax.sql.DataSource;
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

@PlatformIntegrationTest
@WithMySql
@Import(MigrationSchemaValidationTestConfiguration.class)
@TestPropertySource(properties = {
    "spring.flyway.target=55",
    "platform.schema-validation.enabled=false",
    "spring.jpa.hibernate.ddl-auto=none"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class EnvironmentTypeCodeMigrationIT {

    @Autowired
    private DataSource dataSource;

    @Test
    void preservesExistingHierarchyAndCompatibilityIdentifiersWhenReplacingTypeIdsWithCodes() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        for (String lifecycle : new String[] { "ACTIVE", "INACTIVE", "QUARANTINED" }) {
            jdbc.update("INSERT IGNORE INTO type_life_cycle (code, label, sort_order, is_active, settings) VALUES (?, ?, 1, true, '{}')",
                lifecycle, lifecycle);
        }
        jdbc.update("INSERT IGNORE INTO type_authorizations (code, label, sort_order, is_active, settings) VALUES ('DEV', 'Dev', 1, true, '{}')");
        jdbc.update("INSERT IGNORE INTO type_workspaces (code, label, sort_order, is_active, settings) VALUES ('MANAGER', 'Manager', 1, true, '{}')");
        jdbc.update("""
            INSERT INTO workspaces (id, version, identifier, workspace_type_code, name, description, requester,
                acronym, settings, email_group, onboarding, lifecycle_code, created_at, updated_at)
            VALUES (100, 0, UUID(), 'MANAGER', 'Environment migration', 'Existing workspace', 'requester',
                'ENV', '{}', 'migration@example.com', false, 'ACTIVE', NOW(6), NOW(6))
            """);
        int typeId = 10;
        for (String code : new String[] { "DEFAULT", "CUSTOM", "SHARD", "CELL" }) {
            jdbc.update("""
                INSERT INTO environment_types (id, version, identifier, code, name, description, root_allowed,
                    allows_children, workspace_required, lifecycle_code, display_order, created_at, updated_at)
                VALUES (?, 7, UUID(), ?, ?, 'Existing type', ?, ?, ?, 'ACTIVE', ?, NOW(6), NOW(6))
                """, typeId, code, code, code.equals("DEFAULT") || code.equals("CUSTOM"),
                !code.equals("CELL"), !code.equals("DEFAULT"), typeId);
            typeId++;
        }
        jdbc.update("""
            INSERT INTO environments (id, version, identifier, workspace_id, environment_type_id, parent_environment_id,
                name, description, authorization_type_code, settings, sort_order, lifecycle_code, created_at, updated_at)
            VALUES (200, 3, 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1', null, 10, null,
                'Existing global', 'Existing global environment', 'DEV', '{"preserved":true}', 1, 'ACTIVE', NOW(6), NOW(6)),
                (201, 4, 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb1', 100, 12, 200,
                'Existing shard', 'Existing workspace shard', 'DEV', '{}', 1, 'INACTIVE', NOW(6), NOW(6)),
                (202, 5, 'cccccccc-cccc-4ccc-8ccc-ccccccccccc1', 100, 13, 201,
                'Existing cell', 'Existing quarantined cell', 'DEV', '{}', 1, 'QUARANTINED', NOW(6), NOW(6))
            """);
        String compatibilityIdentifier = "dddddddd-dddd-4ddd-8ddd-ddddddddddd1";
        jdbc.update("""
            INSERT INTO environment_type_compatibilities (id, identifier, parent_type_id, child_type_id,
                lifecycle_code, created_at, updated_at)
            VALUES (300, ?, 12, 13, 'INACTIVE', NOW(6), NOW(6))
            """, compatibilityIdentifier);
        jdbc.update("INSERT IGNORE INTO type_schema_scopes (code, label, sort_order, is_active, settings) VALUES ('PLATFORM', 'Platform', 1, true, '{}')");
        jdbc.update("INSERT IGNORE INTO type_schema_version_status (code, label, sort_order, is_active, settings) VALUES ('PUBLISHED', 'Published', 1, true, '{}')");
        for (String resource : new String[] { "environment", "environment-type", "environment-type-compatibility" }) {
            jdbc.update("""
                INSERT IGNORE INTO schema_definitions (version, identifier, scope_code, code, name,
                    lifecycle_code, created_at, updated_at)
                VALUES (0, UUID(), 'PLATFORM', ?, ?, 'ACTIVE', NOW(6), NOW(6))
                """, resource + "-resource", resource);
        }

        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();

        assertThat(jdbc.queryForList("SELECT code FROM environment_types ORDER BY code", String.class))
            .containsExactly("CELL", "CUSTOM", "DEFAULT", "SHARD");
        assertThat(jdbc.queryForList("SELECT version FROM environment_types ORDER BY code", Long.class))
            .containsOnly(7L);
        assertThat(jdbc.queryForList("SELECT environment_type_code FROM environments ORDER BY id", String.class))
            .containsExactly("DEFAULT", "SHARD", "CELL");
        assertThat(jdbc.queryForList("SELECT lifecycle_code FROM environments ORDER BY id", String.class))
            .containsExactly("ACTIVE", "INACTIVE", "QUARANTINED");
        assertThat(jdbc.queryForList("SELECT version FROM environments ORDER BY id", Long.class))
            .containsExactly(3L, 4L, 5L);
        assertThat(jdbc.queryForObject("SELECT parent_environment_id FROM environments WHERE id=202", Long.class)).isEqualTo(201L);
        assertThat(jdbc.queryForObject("SELECT workspace_id FROM environments WHERE id=201", Long.class)).isEqualTo(100L);
        assertThat(jdbc.queryForObject("SELECT JSON_EXTRACT(settings, '$.preserved') FROM environments WHERE id=200", String.class))
            .isEqualTo("true");
        assertThat(jdbc.queryForMap("SELECT identifier, parent_type_code, child_type_code, lifecycle_code FROM environment_type_compatibilities WHERE id=300"))
            .containsEntry("identifier", compatibilityIdentifier)
            .containsEntry("parent_type_code", "SHARD")
            .containsEntry("child_type_code", "CELL")
            .containsEntry("lifecycle_code", "INACTIVE");
        assertThat(jdbc.queryForObject("""
            SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=database()
                AND TABLE_NAME='environment_types' AND COLUMN_NAME IN ('id', 'identifier')
            """, Integer.class)).isZero();
        assertThat(jdbc.queryForObject("""
            SELECT COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=database()
                AND TABLE_NAME='environment_types' AND CONSTRAINT_NAME='PRIMARY'
            """, String.class)).isEqualTo("code");
        assertThatThrownBy(() -> jdbc.update("UPDATE environment_types SET workspace_required=true WHERE code='DEFAULT'"))
            .hasRootCauseInstanceOf(java.sql.SQLException.class)
            .satisfies(exception -> assertThat(((java.sql.SQLException) exception.getCause()).getErrorCode()).isEqualTo(3819));
        assertThatThrownBy(() -> jdbc.update("UPDATE environments SET environment_type_code='MISSING' WHERE id=201"))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM environment_types WHERE code='CELL'"))
            .isInstanceOf(DataIntegrityViolationException.class);

        JsonMapper mapper = JsonMapper.builder().build();
        for (String resource : new String[] { "environment", "environment-type", "environment-type-compatibility" }) {
            String definition = jdbc.queryForObject("""
                SELECT sv.definition FROM schema_versions sv JOIN schema_definitions sd ON sd.id=sv.schema_id
                WHERE sd.code=? AND sv.status='PUBLISHED' ORDER BY sv.schema_version DESC LIMIT 1
                """, String.class, resource + "-resource");
            try (InputStream stream = new ClassPathResource("schemas/" + resource + ".json").getInputStream()) {
                assertThat(mapper.readTree(definition)).isEqualTo(mapper.readTree(stream));
            }
        }
    }
}
