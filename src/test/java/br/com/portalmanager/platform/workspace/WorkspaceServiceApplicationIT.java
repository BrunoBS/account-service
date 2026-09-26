package br.com.portalmanager.platform.workspace;

import br.com.portalmanager.platform.testing.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.testing.annotation.WithMySql;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

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
