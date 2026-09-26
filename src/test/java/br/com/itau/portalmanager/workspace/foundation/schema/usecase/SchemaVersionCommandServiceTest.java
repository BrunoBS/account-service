package br.com.itau.portalmanager.workspace.foundation.schema.usecase;

import br.com.itau.portalmanager.workspace.foundation.schema.domain.Schema;
import br.com.itau.portalmanager.workspace.foundation.schema.domain.SchemaVersion;
import br.com.itau.portalmanager.workspace.foundation.schema.domain.SchemaVersionStatus;
import br.com.itau.portalmanager.workspace.foundation.schema.repository.SchemaRepository;
import br.com.itau.portalmanager.workspace.foundation.schema.repository.SchemaVersionRepository;
import br.com.itau.portalmanager.workspace.foundation.schema.usecase.model.CreateSchemaVersionInput;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SchemaVersionCommandServiceTest {

    private final SchemaRepository schemaRepository = mock(SchemaRepository.class);
    private final SchemaVersionRepository versionRepository = mock(SchemaVersionRepository.class);
    private final SchemaValidator validator = new SchemaValidator(new ObjectMapper());
    private final SchemaVersionCommandService service =
            new SchemaVersionCommandService(schemaRepository, versionRepository, validator);

    @Test
    void shouldCreateFirstDraftAsVersionOne() throws Exception {
        Schema schema = schema(42L);
        when(schemaRepository.findByIdentifierAndScope("schema-1", "PLATFORM", null))
                .thenReturn(Optional.of(schema));
        when(schemaRepository.findByIdentifierForUpdate("schema-1")).thenReturn(Optional.of(schema));
        when(versionRepository.findAllForUpdate(42L)).thenReturn(List.of());
        when(versionRepository.save(any(SchemaVersion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var output = service.createPlatformDraft(
                "schema-1",
                new CreateSchemaVersionInput(
                        null,
                        new ObjectMapper().readTree("{\"type\":\"object\"}")
                )
        );

        assertThat(output.version()).isEqualTo(1);
        assertThat(output.versionName()).isEqualTo("v1");
        assertThat(output.status()).isEqualTo("DRAFT");
    }

    @Test
    void shouldNotCreateNewVersionWhenDefinitionDidNotChange() throws Exception {
        Schema schema = schema(42L);
        SchemaVersion existing = new SchemaVersion(
                schema,
                1,
                "v1",
                "{\"type\":\"object\"}",
                SchemaVersionStatus.PUBLISHED,
                LocalDateTime.now()
        );

        when(schemaRepository.findByIdentifierAndScope("schema-1", "PLATFORM", null))
                .thenReturn(Optional.of(schema));
        when(schemaRepository.findByIdentifierForUpdate("schema-1")).thenReturn(Optional.of(schema));
        when(versionRepository.findAllForUpdate(42L)).thenReturn(List.of(existing));

        var output = service.createPlatformDraft(
                "schema-1",
                new CreateSchemaVersionInput(
                        "ignored",
                        new ObjectMapper().readTree("{\"type\":\"object\"}")
                )
        );

        assertThat(output.version()).isEqualTo(1);
        assertThat(output.status()).isEqualTo("PUBLISHED");
    }

    @Test
    void shouldCreateNextDraftWhenDefinitionChanges() throws Exception {
        Schema schema = schema(42L);
        SchemaVersion existing = new SchemaVersion(
                schema,
                1,
                "v1",
                "{\"type\":\"object\"}",
                SchemaVersionStatus.PUBLISHED,
                LocalDateTime.now()
        );

        when(schemaRepository.findByIdentifierAndScope("schema-1", "PLATFORM", null))
                .thenReturn(Optional.of(schema));
        when(schemaRepository.findByIdentifierForUpdate("schema-1")).thenReturn(Optional.of(schema));
        when(versionRepository.findAllForUpdate(42L)).thenReturn(List.of(existing));
        when(versionRepository.save(any(SchemaVersion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var output = service.createPlatformDraft(
                "schema-1",
                new CreateSchemaVersionInput(
                        "v2-candidate",
                        new ObjectMapper().readTree("{\"type\":\"object\",\"required\":[\"name\"]}")
                )
        );

        assertThat(output.version()).isEqualTo(2);
        assertThat(output.versionName()).isEqualTo("v2-candidate");
        assertThat(output.status()).isEqualTo("DRAFT");
    }

    @Test
    void shouldPublishDraftIdempotently() {
        Schema schema = schema(42L);
        SchemaVersion draft = new SchemaVersion(
                schema,
                2,
                "v2",
                "{\"type\":\"object\"}",
                SchemaVersionStatus.DRAFT,
                LocalDateTime.now()
        );

        when(schemaRepository.findByIdentifierAndScope("schema-1", "PLATFORM", null))
                .thenReturn(Optional.of(schema));
        when(schemaRepository.findByIdentifierForUpdate("schema-1")).thenReturn(Optional.of(schema));
        when(versionRepository.findAllForUpdate(42L)).thenReturn(List.of(draft));
        when(versionRepository.findByIdentifierAndSchema_Id(draft.getIdentifier(), 42L))
                .thenReturn(Optional.of(draft));

        var output = service.publishPlatform("schema-1", draft.getIdentifier());

        assertThat(output.status()).isEqualTo("PUBLISHED");
        assertThat(draft.isPublished()).isTrue();
    }

    private Schema schema(Long id) {
        Schema schema = mock(Schema.class);
        when(schema.getId()).thenReturn(id);
        return schema;
    }
}
