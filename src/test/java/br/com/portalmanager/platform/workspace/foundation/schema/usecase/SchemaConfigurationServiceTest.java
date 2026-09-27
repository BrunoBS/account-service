package br.com.portalmanager.platform.workspace.foundation.schema.usecase;

import br.com.portalmanager.platform.library.messaging.exception.ConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.*;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.*;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.CreateSchemaConfigurationInput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.configuration.SchemaConfigurationService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SchemaConfigurationServiceTest {
    private final SchemaConfigurationRepository configurations = mock(SchemaConfigurationRepository.class);
    private final SchemaRepository schemas = mock(SchemaRepository.class);
    private final SchemaConfigurationService service = new SchemaConfigurationService(configurations, schemas);

    @Test
    void publisherCodeIsCanonicalAndDoesNotRequireAPreexistingBinding() {
        Schema schema = mock(Schema.class);
        when(schema.getScope()).thenReturn(SchemaScopeTypeCode.platform());
        when(schema.getIdentifier()).thenReturn("schema");
        when(schemas.findByIdentifier("schema")).thenReturn(Optional.of(schema));
        when(configurations.save(any(SchemaConfiguration.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(service.create(new CreateSchemaConfigurationInput("publisher", " web_socket ", "schema"))
                .resourceCode()).isEqualTo("WEB_SOCKET");
        verify(configurations).save(any(SchemaConfiguration.class));
    }

    @Test
    void rejectsDuplicateResourcePair() {
        when(configurations.existsByResourceTypeAndResourceCode(SchemaResourceType.PUBLISHER, "KAAS"))
                .thenReturn(true);
        assertThatThrownBy(() -> service.create(
                new CreateSchemaConfigurationInput("PUBLISHER", "KAAS", "schema")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void cannotBindWorkspaceSchemaAsPlatformConfiguration() {
        Schema schema = mock(Schema.class);
        when(schema.getScope()).thenReturn(SchemaScopeTypeCode.workspace());
        when(schemas.findByIdentifier("schema")).thenReturn(Optional.of(schema));
        assertThatThrownBy(() -> service.create(
                new CreateSchemaConfigurationInput("FEATURE", "shared-keys", "schema")))
                .isInstanceOf(ValidationException.class);
    }
}
