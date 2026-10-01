package br.com.portalmanager.platform.workspace.core.workspace.integration;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.core.workspace.repository.WorkspaceRepository;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceFinder;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.validation.WorkspaceValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.usecase.WorkspaceTypeService;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class DefaultWorkspaceReferenceResolverTest {

    private final WorkspaceRepository repository = mock(WorkspaceRepository.class);
    private final WorkspaceFinder finder = new WorkspaceFinder(repository);
    private final WorkspaceValidator validator = new WorkspaceValidator(mock(WorkspaceTypeService.class));
    private final DefaultWorkspaceReferenceResolver resolver =
            new DefaultWorkspaceReferenceResolver(finder, validator);

    @Test
    void resolvesBothDirectionsForActiveWorkspace() {
        Workspace workspace = mock(Workspace.class);
        when(workspace.getId()).thenReturn(7L);
        when(workspace.getIdentifier()).thenReturn("workspace-identifier");
        when(workspace.getLifecycle()).thenReturn(LifecycleTypeCode.active());
        when(repository.findByIdentifier("workspace-identifier"))
                .thenReturn(Optional.of(workspace));
        when(repository.findById(7L))
                .thenReturn(Optional.of(workspace));

        assertThat(resolver.resolveInternalId("workspace-identifier")).isEqualTo(7L);
        assertThat(resolver.resolveIdentifier(7L)).isEqualTo("workspace-identifier");
    }

    @Test
    void rejectsInactiveQuarantinedAndMissingWorkspacesInBothDirections() {
        for (String state : new String[]{"INACTIVE", "QUARANTINED", "MISSING"}) {
            Workspace workspace = mock(Workspace.class);
            if (!state.equals("MISSING")) {
                when(workspace.getLifecycle()).thenReturn(state.equals("INACTIVE")
                        ? LifecycleTypeCode.inactive() : LifecycleTypeCode.quarantined());
                when(repository.findByIdentifier("workspace-" + state)).thenReturn(Optional.of(workspace));
                when(repository.findById(7L)).thenReturn(Optional.of(workspace));
            } else {
                when(repository.findById(7L)).thenReturn(Optional.empty());
            }
            assertThatThrownBy(() -> resolver.resolveInternalId("workspace-" + state))
                    .isInstanceOf(NotFoundException.class);
            assertThatThrownBy(() -> resolver.resolveIdentifier(7L))
                    .isInstanceOf(NotFoundException.class);
            verify(repository).findByIdentifier("workspace-" + state);
        }
        verify(repository, org.mockito.Mockito.times(3)).findById(7L);
    }
}
