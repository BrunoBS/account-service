package br.com.portalmanager.account;

import br.com.portalmanager.core.testing.annotation.PlatformIntegrationTest;
import br.com.portalmanager.core.testing.annotation.WithMySql;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@PlatformIntegrationTest
@WithMySql
class AccountServiceApplicationIT {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void shouldStartUsingGoldenFoundation() {
        assertNotNull(applicationContext);
    }
}
