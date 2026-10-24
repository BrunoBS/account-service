package br.com.portalmanager.platform.workspace.entrypoint.workspace;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultFixture;
import org.springframework.jdbc.core.JdbcTemplate;

/** Shared catalogue and authorization setup for workspace HTTP scenarios. */
final class WorkspaceScenario {
    private final JdbcTemplate jdbcTemplate;
    private final AuthorizationMock authorizationMock;

    WorkspaceScenario(JdbcTemplate jdbcTemplate, AuthorizationMock authorizationMock) {
        this.jdbcTemplate = jdbcTemplate;
        this.authorizationMock = authorizationMock;
    }

    void ownerCanManageWorkspaces() {
        SchemaDefaultFixture.seed(jdbcTemplate);
        seedWorkspaceTypes();
        seedLifecycleTypes();
        authorizationMock.reset();
        authorizationMock.allow(session -> session.userName("test-user").groups("PM5_OWNER"));
    }

    private void seedWorkspaceTypes() {
        jdbcTemplate.update(
            "INSERT IGNORE INTO type_workspaces (code, label, description, sort_order, is_active, settings) VALUES ('ADMIN', 'Admin', 'Administrative workspace', 1, true, '{}')"
        );
        jdbcTemplate.update(
            "INSERT IGNORE INTO type_workspaces (code, label, description, sort_order, is_active, settings) VALUES ('MANAGER', 'Manager', 'Management workspace', 2, true, '{}')"
        );
        jdbcTemplate.update(
            "INSERT IGNORE INTO type_workspaces (code, label, description, sort_order, is_active, settings) VALUES ('CATALOG', 'Catalog', 'Catalog workspace', 3, true, '{}')"
        );
    }

    private void seedLifecycleTypes() {
        jdbcTemplate.update(
            "INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings) VALUES ('ACTIVE', 'Active', 'Active lifecycle state', 1, true, '{}')"
        );
        jdbcTemplate.update(
            "INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings) VALUES ('INACTIVE', 'Inactive', 'Inactive lifecycle state', 2, true, '{}')"
        );
        jdbcTemplate.update(
            "INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings) VALUES ('QUARANTINED', 'Quarantined', 'Quarantined lifecycle state', 3, true, '{}')"
        );
    }
}
