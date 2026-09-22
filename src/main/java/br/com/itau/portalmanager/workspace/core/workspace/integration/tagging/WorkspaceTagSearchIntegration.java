package br.com.itau.portalmanager.workspace.core.workspace.integration.tagging;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WorkspaceTagSearchIntegration {

    private final JdbcTemplate jdbcTemplate;

    public WorkspaceTagSearchIntegration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> findOwnerIdsByTag(String normalizedTag) {
        return jdbcTemplate.queryForList(
                """
                select distinct owner_id
                  from tags
                 where owner_type = 'WORKSPACE'
                   and lower(name) like lower(?)
                 order by owner_id
                """,
                String.class,
                "%" + normalizedTag + "%"
        );
    }
}
