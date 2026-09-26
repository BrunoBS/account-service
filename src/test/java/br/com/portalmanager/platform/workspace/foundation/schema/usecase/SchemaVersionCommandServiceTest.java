package br.com.portalmanager.platform.workspace.foundation.schema.usecase;

import br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.domain.SchemaVersionStatusTypeCode;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.Schema;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaVersion;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaVersionRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation.SchemaValidator;
import br.com.portalmanager.platform.workspace.foundation.integration.WorkspaceReferenceResolver;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.version.SchemaVersionCommandService;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.CreateSchemaVersionInput;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SchemaVersionCommandServiceTest {

    private final SchemaRepository schemaRepository = mock(SchemaRepository.class);
    private final SchemaVersionRepository versionRepository = mock(SchemaVersionRepository.class);
    private final SchemaValidator validator = new SchemaValidator(new ObjectMapper());
    private final WorkspaceReferenceResolver workspaceReferenceResolver = mock(WorkspaceReferenceResolver.class);
    private final SchemaVersionCommandService service =
            new SchemaVersionCommandService(schemaRepository, versionRepository, validator, workspaceReferenceResolver);

    @Test
    void shouldContinueEditingExistingDraftInsteadOfCreatingAnotherVersion() throws Exception {
        Schema schema = schema(42L);
        SchemaVersion draft = new SchemaVersion(
                schema,
                1,
                "v1",
                "{\"type\":\"object\"}",
                SchemaVersionStatusTypeCode.draft(),
                LocalDateTime.now()
        );

        when(schemaRepository.findByIdentifierAndScopeForUpdate("schema-1", "PLATFORM", null))
                .thenReturn(Optional.of(schema));
        when(versionRepository.findAllForUpdate(42L)).thenReturn(List.of(draft));

        var output = service.createPlatformDraft(
                "schema-1",
                new CreateSchemaVersionInput(
                        "v1-edit",
                        new ObjectMapper().readTree("{\"type\":\"object\",\"required\":[\"name\"]}")
                )
        );

        assertThat(output.identifier()).isEqualTo(draft.getIdentifier());
        assertThat(output.version()).isEqualTo(1);
        assertThat(output.versionName()).isEqualTo("v1-edit");
        assertThat(output.status()).isEqualTo("DRAFT");
        verify(versionRepository, never()).save(any(SchemaVersion.class));
    }

    @Test
    void shouldCreateNextDraftWhenEditingPublishedVersionEvenWithoutDefinitionChange() throws Exception {
        Schema schema = schema(42L);
        SchemaVersion published = published(schema, 1, "{\"type\":\"object\"}");

        when(schemaRepository.findByIdentifierAndScopeForUpdate("schema-1", "PLATFORM", null))
                .thenReturn(Optional.of(schema));
        when(versionRepository.findAllForUpdate(42L)).thenReturn(List.of(published));
        when(versionRepository.save(any(SchemaVersion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var output = service.createPlatformDraft(
                "schema-1",
                new CreateSchemaVersionInput(
                        "v2",
                        new ObjectMapper().readTree("{\"type\":\"object\"}")
                )
        );

        assertThat(output.version()).isEqualTo(2);
        assertThat(output.status()).isEqualTo("DRAFT");
        assertThat(output.definition()).isEqualTo(published.getDefinition());
        verify(versionRepository).save(any(SchemaVersion.class));
    }

    @Test
    void shouldCreateNextDraftWhenPublishedDefinitionChanges() throws Exception {
        Schema schema = schema(42L);
        SchemaVersion published = published(schema, 1, "{\"type\":\"object\"}");

        when(schemaRepository.findByIdentifierAndScopeForUpdate("schema-1", "PLATFORM", null))
                .thenReturn(Optional.of(schema));
        when(versionRepository.findAllForUpdate(42L)).thenReturn(List.of(published));
        when(versionRepository.save(any(SchemaVersion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var output = service.createPlatformDraft(
                "schema-1",
                new CreateSchemaVersionInput(
                        "v2",
                        new ObjectMapper().readTree("{\"type\":\"object\",\"required\":[\"name\"]}")
                )
        );

        assertThat(output.version()).isEqualTo(2);
        assertThat(output.status()).isEqualTo("DRAFT");
        assertThat(output.definition()).contains("required");
    }

    @Test
    void shouldReuseDiscardedDraftVersionNumber() throws Exception {
        Schema schema = schema(42L);
        SchemaVersion published = published(schema, 1, "{\"type\":\"object\"}");

        when(schemaRepository.findByIdentifierAndScopeForUpdate("schema-1", "PLATFORM", null))
                .thenReturn(Optional.of(schema));
        when(versionRepository.findAllForUpdate(42L)).thenReturn(List.of(published));
        when(versionRepository.save(any(SchemaVersion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var output = service.createPlatformDraft(
                "schema-1",
                new CreateSchemaVersionInput(
                        "v2-recreated",
                        new ObjectMapper().readTree("{\"type\":\"object\",\"required\":[\"name\"]}")
                )
        );

        assertThat(output.version()).isEqualTo(2);
        assertThat(output.status()).isEqualTo("DRAFT");
    }

    @Test
    void shouldPublishDraft() {
        Schema schema = schema(42L);
        SchemaVersion draft = new SchemaVersion(
                schema,
                2,
                "v2",
                "{\"type\":\"object\"}",
                SchemaVersionStatusTypeCode.draft(),
                LocalDateTime.now()
        );

        when(schemaRepository.findByIdentifierAndScopeForUpdate("schema-1", "PLATFORM", null))
                .thenReturn(Optional.of(schema));
        when(versionRepository.findAllForUpdate(42L)).thenReturn(List.of(draft));
        when(versionRepository.findByIdentifierAndSchema_Id(draft.getIdentifier(), 42L))
                .thenReturn(Optional.of(draft));

        var output = service.publishPlatform("schema-1", draft.getIdentifier());

        assertThat(output.status()).isEqualTo("PUBLISHED");
        assertThat(draft.isPublished()).isTrue();
    }

    @Test
    void shouldDeleteDraftPhysically() {
        Schema schema = schema(42L);
        SchemaVersion draft = new SchemaVersion(
                schema,
                2,
                "v2",
                "{\"type\":\"object\"}",
                SchemaVersionStatusTypeCode.draft(),
                LocalDateTime.now()
        );

        when(schemaRepository.findByIdentifierAndScopeForUpdate("schema-1", "PLATFORM", null))
                .thenReturn(Optional.of(schema));
        when(versionRepository.findAllForUpdate(42L)).thenReturn(List.of(draft));
        when(versionRepository.findByIdentifierAndSchema_Id(draft.getIdentifier(), 42L))
                .thenReturn(Optional.of(draft));

        service.deletePlatformDraft("schema-1", draft.getIdentifier());

        verify(versionRepository).delete(draft);
    }

    @Test
    void shouldRejectPhysicalDeleteOfPublishedVersion() {
        Schema schema = schema(42L);
        SchemaVersion published = published(schema, 1, "{\"type\":\"object\"}");

        when(schemaRepository.findByIdentifierAndScopeForUpdate("schema-1", "PLATFORM", null))
                .thenReturn(Optional.of(schema));
        when(versionRepository.findAllForUpdate(42L)).thenReturn(List.of(published));
        when(versionRepository.findByIdentifierAndSchema_Id(published.getIdentifier(), 42L))
                .thenReturn(Optional.of(published));

        assertThatThrownBy(() ->
                service.deletePlatformDraft("schema-1", published.getIdentifier())
        ).isInstanceOf(IllegalStateException.class);

        verify(versionRepository, never()).delete(any(SchemaVersion.class));
    }

    private SchemaVersion published(Schema schema, int version, String definition) {
        return new SchemaVersion(
                schema,
                version,
                "v" + version,
                definition,
                SchemaVersionStatusTypeCode.published(),
                LocalDateTime.now()
        );
    }

    private Schema schema(Long id) {
        Schema schema = mock(Schema.class);
        when(schema.getId()).thenReturn(id);
        return schema;
    }
}
