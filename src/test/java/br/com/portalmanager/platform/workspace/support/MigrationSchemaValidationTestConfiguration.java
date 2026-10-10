package br.com.portalmanager.platform.workspace.support;

import br.com.portalmanager.platform.library.schemavalidation.repository.ResourceSchemaRepository;
import java.util.Optional;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class MigrationSchemaValidationTestConfiguration {

    @Bean
    ResourceSchemaRepository migrationTestResourceSchemaRepository() {
        return (resourceType, resourceCode) -> Optional.empty();
    }
}
