package br.com.portalmanager.platform.workspace.feature.sharing.repository;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.support.MigrationSchemaValidationTestConfiguration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

@PlatformIntegrationTest
@Import(MigrationSchemaValidationTestConfiguration.class)
@WithMySql
@TestPropertySource(
        properties = {
            "spring.flyway.target=79",
            "platform.schema-validation.enabled=false",
            "spring.jpa.hibernate.ddl-auto=none",
        })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ResourceSharingRenameMigrationIT {

    @Autowired private DataSource dataSource;

    @Autowired private Flyway flyway;

    @Test
    void migratesExistingReferencesWithoutChangingPublicIdentifiersOrRelationships() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("79");
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
                       (102, 'participant-workspace', 'MANAGER', 'Participant', 'Participant', 'origin', 'PART', '{}', 'participant@test', 'ACTIVE', NOW(6), NOW(6))
                """);
        jdbc.update(
                """
                INSERT INTO applications (id, identifier, workspace_id, name, alias, acronym, application_scope_code,
                    settings, lifecycle_code, created_at, updated_at)
                VALUES (201, 'owner-application', 101, 'Owner', 'owner', 'OWN', 'BACKEND', '{}', 'ACTIVE', NOW(6), NOW(6)),
                       (202, 'participant-application', 102, 'Participant', 'participant', 'PART', 'BACKEND', '{}', 'ACTIVE', NOW(6), NOW(6))
                """);
        jdbc.update(
                "INSERT INTO"
                    + " service_context_microservices(id,identifier,code,name,settings,lifecycle_code,created_at,updated_at)"
                    + " VALUES (601,'sharing-service','sharing-service','Sharing"
                    + " Service','{}','ACTIVE',NOW(6),NOW(6))");
        jdbc.update(
                "INSERT INTO"
                    + " service_context_features(id,identifier,code,name,microservice_id,settings,lifecycle_code,shareable,created_at,updated_at)"
                    + " VALUES (602,'sharing-feature','sharing-feature','Sharing"
                    + " Feature',601,'{}','ACTIVE',true,NOW(6),NOW(6))");
        jdbc.update(
                "INSERT IGNORE INTO"
                    + " environment_types(code,name,description,root_allowed,workspace_required,lifecycle_code,display_order,created_at,updated_at)"
                    + " VALUES"
                    + " ('DEFAULT','Default','Default',true,false,'ACTIVE',1,NOW(6),NOW(6))");
        jdbc.update(
                "INSERT INTO"
                    + " environments(id,identifier,environment_type_code,name,description,authorization_type_code,settings,sort_order,lifecycle_code,created_at,updated_at)"
                    + " VALUES"
                    + " (301,'source-environment','DEFAULT','Source','Source','DEV','{}',1,'ACTIVE',NOW(6),NOW(6)),(302,'destination-environment','DEFAULT','Destination','Destination','DEV','{}',1,'ACTIVE',NOW(6),NOW(6))");
        jdbc.update(
                "INSERT INTO"
                    + " shared_contracts(id,identifier,version,owner_workspace_id,owner_application_id,feature_id,lifecycle_code,created_at,updated_at)"
                    + " VALUES (401,'contract',3,101,201,602,'ACTIVE',NOW(6),NOW(6))");
        jdbc.update(
                "INSERT INTO"
                    + " shared_participations(id,identifier,version,contract_id,participant_workspace_id,participant_application_id,status_code,created_at,updated_at)"
                    + " VALUES (501,'participant',7,401,102,202,'PENDING',NOW(6),NOW(6))");
        jdbc.update(
                "INSERT INTO"
                    + " shared_environment_mappings(participation_id,source_environment_id,destination_environment_id,created_at,updated_at)"
                    + " VALUES (501,301,302,NOW(6),NOW(6))");
        java.util.List<java.util.Map<String, Object>> contracts =
                jdbc.queryForList("SELECT * FROM shared_contracts");
        java.util.List<java.util.Map<String, Object>> participants =
                jdbc.queryForList("SELECT * FROM shared_participations");
        java.util.List<java.util.Map<String, Object>> mappings =
                jdbc.queryForList(
                        "SELECT id,participation_id AS"
                            + " participant_id,source_environment_id,destination_environment_id,created_at,updated_at"
                            + " FROM shared_environment_mappings");
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target("80")
                .load()
                .migrate();
        assertThat(jdbc.queryForList("SELECT * FROM resource_sharing_contracts"))
                .isEqualTo(contracts);
        assertThat(jdbc.queryForList("SELECT * FROM resource_sharing_participants"))
                .isEqualTo(participants);
        assertThat(
                        jdbc.queryForList(
                                "SELECT"
                                    + " id,participant_id,source_environment_id,destination_environment_id,created_at,updated_at"
                                    + " FROM resource_sharing_environment_mappings"))
                .isEqualTo(mappings);
        jdbc.update("DELETE FROM resource_sharing_contracts WHERE id=401");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM resource_sharing_participants",
                                Integer.class))
                .isZero();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM resource_sharing_environment_mappings",
                                Integer.class))
                .isZero();
    }

    private void seedCatalog(JdbcTemplate jdbc, String table, String code) {
        jdbc.update(
                "INSERT IGNORE INTO "
                        + table
                        + " (code, label, description, sort_order, is_active, settings) VALUES (?,"
                        + " ?, ?, 1, true, '{}')",
                code,
                code,
                code);
    }
}
