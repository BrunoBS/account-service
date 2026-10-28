package br.com.portalmanager.platform.workspace.integration.foundation.operations;

import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;

/** Verifies actual MySQL metadata after Flyway creates the empty container database. */
class FreshSchemaIntegrationTest extends BaseIntegrationTest {
    @Autowired private DataSource dataSource;

    @Test void freshDatabaseContainsCoreTablesConstraintsAndRuntimeViews() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            Set<String> tables = new HashSet<>();
            try (ResultSet result = metadata.getTables(connection.getCatalog(), null, "%", null)) {
                while (result.next()) tables.add(result.getString("TABLE_NAME"));
            }
            assertThat(tables).contains("workspaces", "applications", "environments", "workspace_environment_configurations", "application_environment_configurations", "flyway_schema_history", "vw_platform_resource_schemas", "vw_dynamic_bundle_messages");
            assertThat(tables).doesNotContain("parameter_publications");
            Set<String> indexes = new HashSet<>();
            try (ResultSet result = metadata.getIndexInfo(connection.getCatalog(), null, "workspace_environment_configurations", true, false)) {
                while (result.next()) indexes.add(result.getString("INDEX_NAME"));
            }
            assertThat(indexes).contains("uk_workspace_env_config_owner_environment");
            Set<String> referencedTables = new HashSet<>();
            try (ResultSet result = metadata.getImportedKeys(connection.getCatalog(), null, "application_environment_configurations")) {
                while (result.next()) referencedTables.add(result.getString("PKTABLE_NAME"));
            }
            assertThat(referencedTables).contains("applications", "environments");
        }
    }
}
