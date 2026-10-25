package br.com.portalmanager.platform.workspace.unit.core.workspaceconfiguration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.operations.WorkspaceEnvironmentConfigurationScope;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.port.WorkspaceReferencePort;
import org.junit.jupiter.api.Test;

class EnvironmentConfigurationScopeTest {
    private final WorkspaceReferencePort workspaces = mock(WorkspaceReferencePort.class);
    private final WorkspaceEnvironmentConfigurationScope scopes = new WorkspaceEnvironmentConfigurationScope(workspaces);

    @Test
    void resolvesActiveWorkspaceForQueries() {
        when(workspaces.resolveActive("workspace")).thenReturn(new WorkspaceReferencePort.ResolvedWorkspace(4L, "workspace", "MANAGER"));
        assertThat(scopes.resolve("workspace", false)).isEqualTo(new WorkspaceEnvironmentConfigurationScope.Scope(4L, 4L));
        verify(workspaces).resolveActive("workspace");
        verify(workspaces, never()).resolveActiveForUpdate(anyString());
    }

    @Test
    void resolvesWorkspaceForMutationsUsingUpdateGuard() {
        when(workspaces.resolveActiveForUpdate("workspace")).thenReturn(new WorkspaceReferencePort.ResolvedWorkspace(5L, "workspace", "MANAGER"));
        assertThat(scopes.resolve("workspace", true)).isEqualTo(new WorkspaceEnvironmentConfigurationScope.Scope(5L, 5L));
        verify(workspaces).resolveActiveForUpdate("workspace");
        verify(workspaces, never()).resolveActive(anyString());
    }
}
