package br.com.portalmanager.platform.workspace.unit.core.workspace.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.core.workspace.repository.projection.WorkspaceReference;
import br.com.portalmanager.platform.workspace.core.workspace.integration.SchemaWorkspaceReferenceAdapter;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceFinder;
import org.junit.jupiter.api.Test;

class SchemaWorkspaceReferenceAdapterTest {

    private final WorkspaceFinder finder = mock(WorkspaceFinder.class);
    private final SchemaWorkspaceReferenceAdapter adapter = new SchemaWorkspaceReferenceAdapter(finder);

    @Test
    void resolvesBothDirectionsAndTypeWithoutLoadingTheAggregate() {
        WorkspaceReference reference = mock(WorkspaceReference.class);
        when(reference.getId()).thenReturn(7L);
        when(reference.getIdentifier()).thenReturn("workspace-identifier");
        when(reference.getWorkspaceType()).thenReturn("ADMIN");
        when(finder.findActiveReference("workspace-identifier")).thenReturn(reference);
        when(finder.findActiveReference(7L)).thenReturn(reference);
        assertThat(adapter.resolveInternalId("workspace-identifier")).isEqualTo(7L);
        assertThat(adapter.resolveIdentifier(7L)).isEqualTo("workspace-identifier");
        verify(finder).findActiveReference("workspace-identifier");
        verify(finder).findActiveReference(7L);
    }

    @Test
    void rejectsReferencesThatAreNotActiveOrDoNotExist() {
        when(finder.findActiveReference("unavailable")).thenThrow(new NotFoundException("not found"));
        when(finder.findActiveReference(7L)).thenThrow(new NotFoundException("not found"));
        assertThatThrownBy(() -> adapter.resolveInternalId("unavailable")).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> adapter.resolveIdentifier(7L)).isInstanceOf(NotFoundException.class);
    }
}
