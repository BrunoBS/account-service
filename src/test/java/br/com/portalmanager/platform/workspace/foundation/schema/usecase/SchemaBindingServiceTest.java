package br.com.portalmanager.platform.workspace.foundation.schema.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.portalmanager.platform.library.messaging.exception.ConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.*;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.binding.SchemaBinding;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.schema.Schema;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.binding.*;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.schema.*;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.binding.CreateSchemaBindingInput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.binding.UpdateSchemaBindingInput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.binding.SchemaBindingService;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation.binding.SchemaBindingValidator;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.mapping.binding.SchemaBindingMapper;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SchemaBindingServiceTest {

    private final SchemaBindingRepository configurations = mock(SchemaBindingRepository.class);
    private final SchemaRepository schemas = mock(SchemaRepository.class);
    private final SchemaBindingService service = new SchemaBindingService(
        configurations,
        schemas,
        new SchemaBindingValidator(),
        new SchemaBindingMapper()
    );

    @Test
    void publisherCodeIsStoredExactlyAsProvided() {
        Schema schema = mock(Schema.class);
        when(schema.getScope()).thenReturn(SchemaScopeTypeCode.platform());
        when(schema.getIdentifier()).thenReturn("schema");
        when(schemas.findByIdentifier("schema")).thenReturn(Optional.of(schema));
        when(configurations.save(any(SchemaBinding.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(
            service.create(new CreateSchemaBindingInput("PUBLISHER", "web_socket", "schema")).resourceCode()
        ).isEqualTo("web_socket");
        verify(configurations).save(any(SchemaBinding.class));
    }

    @Test
    void rejectsDuplicateResourcePair() {
        when(configurations.existsByResourceTypeAndResourceCode("PUBLISHER", "KAAS")).thenReturn(true);
        assertThatThrownBy(() ->
            service.create(new CreateSchemaBindingInput("PUBLISHER", "KAAS", "schema"))
        ).isInstanceOf(ConflictException.class);
    }

    @Test
    void cannotBindWorkspaceSchemaAsPlatformConfiguration() {
        Schema schema = mock(Schema.class);
        when(schema.getScope()).thenReturn(SchemaScopeTypeCode.workspace());
        when(schemas.findByIdentifier("schema")).thenReturn(Optional.of(schema));
        assertThatThrownBy(() ->
            service.create(new CreateSchemaBindingInput("FEATURE", "shared-keys", "schema"))
        ).isInstanceOf(ValidationException.class);
    }

    @Test
    void updateRequiresCurrentVersionBeforeLookingUpSchema() {
        SchemaBinding configuration = mock(SchemaBinding.class);
        when(configurations.findByIdentifier("configuration")).thenReturn(Optional.of(configuration));
        when(configuration.getVersion()).thenReturn(3L);

        assertThatThrownBy(() ->
            service.update("configuration", new UpdateSchemaBindingInput(null, "schema"))
        ).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() ->
            service.update("configuration", new UpdateSchemaBindingInput(2L, "schema"))
        ).isInstanceOf(ResourceVersionConflictException.class);
        verifyNoInteractions(schemas);
    }

    @Test
    void activeConfigurationCannotBeDeleted() {
        SchemaBinding configuration = mock(SchemaBinding.class);
        when(configurations.findByIdentifier("configuration")).thenReturn(Optional.of(configuration));
        when(configuration.isActive()).thenReturn(true);

        assertThatThrownBy(() -> service.delete("configuration")).isInstanceOf(ConflictException.class);
        verify(configurations, never()).delete(any(SchemaBinding.class));
    }
}
