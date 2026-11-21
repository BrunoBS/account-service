package br.com.portalmanager.platform.workspace.feature.authorizationcache.integration;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AuthorizationViewRepository {
    private final JdbcClient jdbc;

    public AuthorizationViewRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<UUID> findWorkspacePage(String cursor) {
        return jdbc.sql("""
                SELECT identifier FROM workspaces
                WHERE identifier > :cursor
                ORDER BY identifier LIMIT 100
                """).param("cursor", cursor)
                .query((row, index) -> UUID.fromString(row.getString(1))).list();
    }

    public List<AuthorizationGroup> findGroups(UUID workspace) {
        return jdbc.sql("""
                SELECT DISTINCT environmentIdentifier, applicationIdentifier, authorizerGroup
                FROM vw_authorizations
                WHERE workspaceIdentifier = :workspace
                """).param("workspace", workspace.toString())
                .query((row, index) -> new AuthorizationGroup(
                        identifier(row.getString(1)), identifier(row.getString(2)), row.getString(3)))
                .list();
    }

    private UUID identifier(String value) {
        return value == null ? null : UUID.fromString(value);
    }

    public record AuthorizationGroup(UUID environment, UUID application, String group) {}
}
