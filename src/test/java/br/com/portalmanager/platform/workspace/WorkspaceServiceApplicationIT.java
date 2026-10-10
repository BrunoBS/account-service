package br.com.portalmanager.platform.workspace;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

@PlatformIntegrationTest
@WithMySql
class WorkspaceServiceApplicationIT {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void shouldStartUsingGoldenFoundation() {
        assertNotNull(applicationContext);
    }
}
