package br.com.portalmanager.platform.workspace.feature.shared.repository;

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
@Import(MigrationSchemaValidationTestConfiguration.class)
@WithMySql
@TestPropertySource(
    properties = {
        "spring.flyway.target=38",
        "platform.schema-validation.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
    }
)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SharedInternalReferencesMigrationIT {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway flyway;

    @Test
    void migratesExistingReferencesWithoutChangingPublicIdentifiersOrRelationships() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("38");
        seedCatalog(jdbc, "type_life_cycle", "ACTIVE");
        seedCatalog(jdbc, "type_workspaces", "MANAGER");
        seedCatalog(jdbc, "type_application_scopes", "BACKEND");
        seedCatalog(jdbc, "type_authorizations", "DEV");
        seedCatalog(jdbc, "type_sharing_statuses", "PENDING");
        jdbc.update(
            """
            INSERT INTO workspaces (id, identifier, workspace_type_code, name, description, requester,
                acronym, settings, email_group, lifecycle_code, created_at, updated_at)
            VALUES (101, 'owner-workspace', 'MANAGER', 'Owner', 'Owner', 'owner', 'OWN', '{}', 'owner@test', 'ACTIVE', NOW(6), NOW(6)),
                   (102, 'participant-workspace', 'MANAGER', 'Participant', 'Participant', 'participant', 'PART', '{}', 'participant@test', 'ACTIVE', NOW(6), NOW(6))
            """
        );
        jdbc.update(
            """
            INSERT INTO applications (id, identifier, workspace_id, name, alias, acronym, application_scope_code,
                settings, lifecycle_code, created_at, updated_at)
            VALUES (201, 'owner-application', 101, 'Owner', 'owner', 'OWN', 'BACKEND', '{}', 'ACTIVE', NOW(6), NOW(6)),
                   (202, 'participant-application', 102, 'Participant', 'participant', 'PART', 'BACKEND', '{}', 'ACTIVE', NOW(6), NOW(6))
            """
        );
        jdbc.update(
            """
            INSERT IGNORE INTO environment_types (identifier, code, name, description, root_allowed,
                workspace_required, lifecycle_code, display_order, created_at, updated_at)
            VALUES (UUID(), 'DEFAULT', 'Default', 'Default', true, false, 'ACTIVE', 1, NOW(6), NOW(6))
            """
        );
        jdbc.update(
            """
            INSERT INTO environments (id, identifier, environment_type_id, name, description,
                authorization_type_code, settings, sort_order, lifecycle_code, created_at, updated_at)
            SELECT 301, 'source-environment', id, 'Source', 'Source', 'DEV', '{}', 1, 'ACTIVE', NOW(6), NOW(6)
            FROM environment_types WHERE code = 'DEFAULT'
            """
        );
        jdbc.update(
            """
            INSERT INTO environments (id, identifier, environment_type_id, name, description,
                authorization_type_code, settings, sort_order, lifecycle_code, created_at, updated_at)
            SELECT 302, 'destination-environment', id, 'Destination', 'Destination', 'DEV', '{}', 1, 'ACTIVE', NOW(6), NOW(6)
            FROM environment_types WHERE code = 'DEFAULT'
            """
        );
        jdbc.update(
            """
            INSERT INTO shared_contracts (id, identifier, owner_workspace_identifier, owner_application_identifier,
                name, lifecycle_code, created_at, updated_at)
            VALUES (401, 'contract', 'owner-workspace', 'owner-application', 'Sharing', 'ACTIVE', NOW(6), NOW(6))
            """
        );
        jdbc.update(
            """
            INSERT INTO shared_participations (id, identifier, contract_id, participant_workspace_identifier,
                participant_application_identifier, status_code, created_at, updated_at)
            VALUES (501, 'participation', 401, 'participant-workspace', 'participant-application', 'PENDING', NOW(6), NOW(6))
            """
        );
        jdbc.update(
            """
            INSERT INTO shared_environment_mappings (participation_id, source_environment_identifier,
                destination_environment_identifier, created_at, updated_at)
            VALUES (501, 'source-environment', 'destination-environment', NOW(6), NOW(6))
            """
        );
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("39").load().migrate();
        assertThat(
            jdbc.queryForMap(
                "SELECT identifier, owner_workspace_id, owner_application_id FROM shared_contracts WHERE id = 401"
            )
        )
            .containsEntry("identifier", "contract")
            .containsEntry("owner_workspace_id", 101L)
            .containsEntry("owner_application_id", 201L);
        assertThat(
            jdbc.queryForMap(
                "SELECT identifier, contract_id, participant_workspace_id, participant_application_id FROM shared_participations WHERE id = 501"
            )
        )
            .containsEntry("identifier", "participation")
            .containsEntry("contract_id", 401L)
            .containsEntry("participant_workspace_id", 102L)
            .containsEntry("participant_application_id", 202L);
        assertThat(
            jdbc.queryForMap(
                "SELECT source_environment_id, destination_environment_id FROM shared_environment_mappings WHERE participation_id = 501"
            )
        )
            .containsEntry("source_environment_id", 301L)
            .containsEntry("destination_environment_id", 302L);
        assertThat(
            jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE()
                    AND table_name IN ('shared_contracts', 'shared_participations', 'shared_environment_mappings')
                    AND column_name IN ('owner_workspace_identifier', 'owner_application_identifier',
                        'participant_workspace_identifier', 'participant_application_identifier',
                        'source_environment_identifier', 'destination_environment_identifier')
                """,
                Integer.class
            )
        ).isZero();
    }

    private void seedCatalog(JdbcTemplate jdbc, String table, String code) {
        jdbc.update(
            "INSERT IGNORE INTO " +
                table +
                " (code, label, description, sort_order, is_active, settings) VALUES (?, ?, ?, 1, true, '{}')",
            code,
            code,
            code
        );
    }
}
