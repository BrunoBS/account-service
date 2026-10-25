package br.com.portalmanager.platform.workspace.unit.core.applicationconfiguration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.operations.ApplicationEnvironmentConfigurationScope;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.port.ApplicationReferencePort;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.port.WorkspaceReferencePort;
import org.junit.jupiter.api.Test;

class EnvironmentConfigurationScopeTest {
    private final WorkspaceReferencePort workspaces = mock(WorkspaceReferencePort.class);
    private final ApplicationReferencePort applications = mock(ApplicationReferencePort.class);
    private final ApplicationEnvironmentConfigurationScope scopes = new ApplicationEnvironmentConfigurationScope(workspaces, applications);

    @Test
    void resolvesActiveWorkspaceAndApplicationForQueries() {
        when(workspaces.resolveActive("workspace")).thenReturn(new WorkspaceReferencePort.ResolvedWorkspace(7L, "workspace", "MANAGER"));
        when(applications.resolveActive("workspace", "app")).thenReturn(9L);
        assertThat(scopes.resolve("workspace", "app", false)).isEqualTo(new ApplicationEnvironmentConfigurationScope.Scope(7L, 9L));
        verify(workspaces).resolveActive("workspace");
        verify(applications).resolveActive("workspace", "app");
        verify(workspaces, never()).resolveActiveForUpdate(anyString());
        verify(applications, never()).resolveActiveForUpdate(anyString(), anyString());
    }

    @Test
    void usesUpdateGuardsForMutations() {
        when(workspaces.resolveActiveForUpdate("workspace")).thenReturn(new WorkspaceReferencePort.ResolvedWorkspace(8L, "workspace", "MANAGER"));
        when(applications.resolveActiveForUpdate("workspace", "app")).thenReturn(10L);
        assertThat(scopes.resolve("workspace", "app", true)).isEqualTo(new ApplicationEnvironmentConfigurationScope.Scope(8L, 10L));
        verify(workspaces).resolveActiveForUpdate("workspace");
        verify(applications).resolveActiveForUpdate("workspace", "app");
        verify(workspaces, never()).resolveActive(anyString());
        verify(applications, never()).resolveActive(anyString(), anyString());
    }
}
