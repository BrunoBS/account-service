package br.com.portalmanager.platform.workspace.foundation.schema.usecase;

import br.com.portalmanager.platform.workspace.foundation.schema.domain.Schema;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaVersionRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.schema.SchemaQueryService;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.workspace.WorkspaceReferenceResolver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SchemaQueryServiceTest {

    private final SchemaRepository schemaRepository = mock(SchemaRepository.class);
    private final SchemaVersionRepository versionRepository = mock(SchemaVersionRepository.class);
    private final WorkspaceReferenceResolver workspaceReferenceResolver =
            mock(WorkspaceReferenceResolver.class);
    private final SchemaQueryService service =
            new SchemaQueryService(schemaRepository, versionRepository, workspaceReferenceResolver);

    @Test
    void shouldScopeWorkspaceVersionReadByResolvedInternalWorkspaceId() {
        Schema schema = mock(Schema.class);
        when(schema.getId()).thenReturn(42L);
        when(workspaceReferenceResolver.resolveInternalId("workspace-identifier"))
                .thenReturn(7L);
        when(schemaRepository.findByIdentifierAndScope(
                "schema-identifier",
                "WORKSPACE",
                7L
        )).thenReturn(Optional.of(schema));
        when(versionRepository.findBySchema_IdOrderBySchemaVersionDesc(42L))
                .thenReturn(List.of());

        var versions = service.findWorkspaceVersions(
                "workspace-identifier",
                "schema-identifier"
        );

        assertThat(versions).isEmpty();
        verify(workspaceReferenceResolver).resolveInternalId("workspace-identifier");
        verify(schemaRepository).findByIdentifierAndScope(
                "schema-identifier",
                "WORKSPACE",
                7L
        );
    }
}
