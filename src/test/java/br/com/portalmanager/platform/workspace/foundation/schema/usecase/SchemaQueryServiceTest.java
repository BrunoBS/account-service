package br.com.portalmanager.platform.workspace.foundation.schema.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import br.com.portalmanager.platform.workspace.foundation.integration.WorkspaceReferencePort;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.schema.Schema;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaVersionRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.SchemaVersionOutput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.schema.SchemaQueryService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SchemaQueryServiceTest {

    private final SchemaRepository schemaRepository = mock(SchemaRepository.class);
    private final SchemaVersionRepository versionRepository = mock(SchemaVersionRepository.class);
    private final WorkspaceReferencePort workspaceReferencePort = mock(WorkspaceReferencePort.class);
    private final SchemaQueryService service = new SchemaQueryService(
        schemaRepository,
        versionRepository,
        workspaceReferencePort
    );

    @Test
    void shouldScopeWorkspaceVersionReadByResolvedInternalWorkspaceId() {
        Schema schema = mock(Schema.class);
        when(schema.getId()).thenReturn(42L);
        when(workspaceReferencePort.resolveInternalId("workspace-identifier")).thenReturn(7L);
        when(schemaRepository.findByIdentifierAndScope("schema-identifier", "WORKSPACE", 7L)).thenReturn(
            Optional.of(schema)
        );
        when(versionRepository.findBySchema_IdOrderBySchemaVersionDesc(42L)).thenReturn(List.of());

        List<SchemaVersionOutput> versions = service.findWorkspaceVersions("workspace-identifier", "schema-identifier");

        assertThat(versions).isEmpty();
        verify(workspaceReferencePort).resolveInternalId("workspace-identifier");
        verify(schemaRepository).findByIdentifierAndScope("schema-identifier", "WORKSPACE", 7L);
    }
}
