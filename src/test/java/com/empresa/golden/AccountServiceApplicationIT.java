package com.empresa.golden;

import com.empresa.platform.testing.annotation.PlatformIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@PlatformIntegrationTest
class AccountServiceApplicationIT {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void shouldStartUsingGoldenFoundation() {
        assertNotNull(applicationContext);
    }
}
