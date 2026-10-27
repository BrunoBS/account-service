package br.com.portalmanager.platform.workspace.integration.support;

import br.com.portalmanager.platform.library.testing.database.DatabaseCleaner;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;

public final class CleanMySqlAfterEachExtension implements AfterEachCallback {
    private static final String[] EXCLUDED_TABLES = {"flyway_schema_history"};

    @Override
    public void afterEach(ExtensionContext context) {
        DataSource dataSource = SpringExtension.getApplicationContext(context).getBean(DataSource.class);
        DatabaseCleaner.clean(dataSource, EXCLUDED_TABLES);
    }
}
