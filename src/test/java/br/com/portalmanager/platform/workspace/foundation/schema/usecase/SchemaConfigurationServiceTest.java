package br.com.portalmanager.platform.workspace.foundation.schema.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.portalmanager.platform.library.messaging.exception.ConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.*;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.configuration.SchemaConfiguration;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.schema.Schema;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.*;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.CreateSchemaConfigurationInput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.UpdateSchemaConfigurationInput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.configuration.SchemaConfigurationService;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation.SchemaConfigurationValidator;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SchemaConfigurationServiceTest {

    private final SchemaConfigurationRepository configurations = mock(SchemaConfigurationRepository.class);
    private final SchemaRepository schemas = mock(SchemaRepository.class);
    private final SchemaConfigurationService service = new SchemaConfigurationService(
        configurations,
        schemas,
        new SchemaConfigurationValidator()
    );

    @Test
    void publisherCodeIsStoredExactlyAsProvided() {
        Schema schema = mock(Schema.class);
        when(schema.getScope()).thenReturn(SchemaScopeTypeCode.platform());
        when(schema.getIdentifier()).thenReturn("schema");
        when(schemas.findByIdentifier("schema")).thenReturn(Optional.of(schema));
        when(configurations.save(any(SchemaConfiguration.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(
            service.create(new CreateSchemaConfigurationInput("PUBLISHER", "web_socket", "schema")).resourceCode()
        ).isEqualTo("web_socket");
        verify(configurations).save(any(SchemaConfiguration.class));
    }

    @Test
    void rejectsDuplicateResourcePair() {
        when(configurations.existsByResourceTypeAndResourceCode("PUBLISHER", "KAAS")).thenReturn(true);
        assertThatThrownBy(() ->
            service.create(new CreateSchemaConfigurationInput("PUBLISHER", "KAAS", "schema"))
        ).isInstanceOf(ConflictException.class);
    }

    @Test
    void cannotBindWorkspaceSchemaAsPlatformConfiguration() {
        Schema schema = mock(Schema.class);
        when(schema.getScope()).thenReturn(SchemaScopeTypeCode.workspace());
        when(schemas.findByIdentifier("schema")).thenReturn(Optional.of(schema));
        assertThatThrownBy(() ->
            service.create(new CreateSchemaConfigurationInput("FEATURE", "shared-keys", "schema"))
        ).isInstanceOf(ValidationException.class);
    }

    @Test
    void updateRequiresCurrentVersionBeforeLookingUpSchema() {
        SchemaConfiguration configuration = mock(SchemaConfiguration.class);
        when(configurations.findByIdentifier("configuration")).thenReturn(Optional.of(configuration));
        when(configuration.getVersion()).thenReturn(3L);

        assertThatThrownBy(() ->
            service.update("configuration", new UpdateSchemaConfigurationInput(null, "schema"))
        ).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() ->
            service.update("configuration", new UpdateSchemaConfigurationInput(2L, "schema"))
        ).isInstanceOf(ResourceVersionConflictException.class);
        verifyNoInteractions(schemas);
    }

    @Test
    void activeConfigurationCannotBeDeleted() {
        SchemaConfiguration configuration = mock(SchemaConfiguration.class);
        when(configurations.findByIdentifier("configuration")).thenReturn(Optional.of(configuration));
        when(configuration.isActive()).thenReturn(true);

        assertThatThrownBy(() -> service.delete("configuration")).isInstanceOf(ConflictException.class);
        verify(configurations, never()).delete(any(SchemaConfiguration.class));
    }
}
