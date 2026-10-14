package br.com.portalmanager.platform.workspace.core.workspace.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.core.workspace.repository.WorkspaceRepository;
import br.com.portalmanager.platform.workspace.core.workspace.repository.projection.WorkspaceReference;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceFinder;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class WorkspaceReferenceAdapterTest {

    private final WorkspaceRepository repository = mock(WorkspaceRepository.class);
    private final WorkspaceFinder finder = new WorkspaceFinder(
        repository,
        mock(jakarta.persistence.EntityManager.class)
    );
    private final WorkspaceReferenceAdapter adapter = new WorkspaceReferenceAdapter(finder);

    @Test
    void resolvesBothDirectionsAndTypeWithoutLoadingTheAggregate() {
        WorkspaceReference reference = mock(WorkspaceReference.class);
        when(reference.getId()).thenReturn(7L);
        when(reference.getIdentifier()).thenReturn("workspace-identifier");
        when(reference.getWorkspaceType()).thenReturn("ADMIN");
        when(repository.findActiveReferenceByIdentifier("workspace-identifier")).thenReturn(Optional.of(reference));
        when(repository.findReferenceById(7L, "ACTIVE")).thenReturn(Optional.of(reference));
        assertThat(adapter.resolveInternalId("workspace-identifier")).isEqualTo(7L);
        assertThat(adapter.resolveIdentifier(7L)).isEqualTo("workspace-identifier");
        assertThat(adapter.resolveActive("workspace-identifier").workspaceType()).isEqualTo("ADMIN");
        verify(repository, never()).findByIdentifier(anyString());
        verify(repository, never()).findById(anyLong());
    }

    @Test
    void rejectsReferencesThatAreNotActiveOrDoNotExist() {
        when(repository.findActiveReferenceByIdentifier("unavailable")).thenReturn(Optional.empty());
        when(repository.findReferenceById(7L, "ACTIVE")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> adapter.resolveInternalId("unavailable")).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> adapter.resolveIdentifier(7L)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> adapter.resolveActive("unavailable")).isInstanceOf(NotFoundException.class);
    }
}
