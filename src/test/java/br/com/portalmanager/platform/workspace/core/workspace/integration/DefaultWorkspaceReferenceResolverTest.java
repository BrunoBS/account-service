package br.com.portalmanager.platform.workspace.core.workspace.integration;

import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.core.workspace.repository.WorkspaceRepository;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceQueryService;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceFinder;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultWorkspaceReferenceResolverTest {

    private final WorkspaceRepository repository = mock(WorkspaceRepository.class);
    private final WorkspaceFinder finder = new WorkspaceFinder(repository);
    private final WorkspaceQueryService queryService =
            new WorkspaceQueryService(repository, finder, null, null, null);
    private final DefaultWorkspaceReferenceResolver resolver =
            new DefaultWorkspaceReferenceResolver(queryService);

    @Test
    void resolvesBothDirectionsForActiveWorkspace() {
        Workspace workspace = mock(Workspace.class);
        when(workspace.getId()).thenReturn(7L);
        when(workspace.getIdentifier()).thenReturn("workspace-identifier");
        when(repository.findByIdentifierAndLifecycleValue("workspace-identifier", "ACTIVE"))
                .thenReturn(Optional.of(workspace));
        when(repository.findByIdAndLifecycleValue(7L, "ACTIVE"))
                .thenReturn(Optional.of(workspace));

        assertThat(resolver.resolveInternalId("workspace-identifier")).isEqualTo(7L);
        assertThat(resolver.resolveIdentifier(7L)).isEqualTo("workspace-identifier");
    }

    @Test
    void rejectsInactiveQuarantinedAndMissingWorkspacesInBothDirections() {
        // A query restricted to ACTIVE returns empty for each of these lifecycle states.
        for (String state : new String[]{"INACTIVE", "QUARANTINED", "MISSING"}) {
            assertThatThrownBy(() -> resolver.resolveInternalId("workspace-" + state))
                    .isInstanceOf(NotFoundException.class);
            assertThatThrownBy(() -> resolver.resolveIdentifier(7L))
                    .isInstanceOf(NotFoundException.class);
            verify(repository).findByIdentifierAndLifecycleValue("workspace-" + state, "ACTIVE");
        }
        verify(repository, org.mockito.Mockito.times(3)).findByIdAndLifecycleValue(7L, "ACTIVE");
    }
}
