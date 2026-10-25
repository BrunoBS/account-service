package br.com.portalmanager.platform.workspace.unit.core.environment.adapter;


import br.com.portalmanager.platform.workspace.core.environment.integration.EnvironmentReferenceAdapter;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.core.environment.domain.environment.Environment;
import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentType;
import br.com.portalmanager.platform.workspace.core.environment.repository.environment.EnvironmentRepository;
import br.com.portalmanager.platform.workspace.core.environment.repository.environment.EnvironmentRepository.IdentifierReference;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment.EnvironmentFinder;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class EnvironmentReferenceAdapterTest {
    private final EnvironmentRepository repository = mock(EnvironmentRepository.class);
    private final EnvironmentFinder finder = mock(EnvironmentFinder.class);
    private final EnvironmentReferenceAdapter adapter = new EnvironmentReferenceAdapter(repository, finder);

    @Test
    void resolvesGlobalDefaultAndWorkspaceReferencesForUpdate() {
        Environment global = mock(Environment.class);
        EnvironmentType defaultType = mock(EnvironmentType.class);
        when(global.getWorkspaceId()).thenReturn(null);
        when(global.getEnvironmentType()).thenReturn(defaultType);
        when(defaultType.getCode()).thenReturn("DEFAULT");
        when(global.getId()).thenReturn(1L);
        when(repository.findByIdentifier("global")).thenReturn(Optional.of(global));
        when(finder.findActiveForUpdate("global", null)).thenReturn(global);
        assertEquals(1L, adapter.resolveActiveForUpdate(9L, "global"));

        Environment scoped = mock(Environment.class);
        EnvironmentType custom = mock(EnvironmentType.class);
        when(scoped.getWorkspaceId()).thenReturn(9L);
        when(scoped.getEnvironmentType()).thenReturn(custom);
        when(custom.getCode()).thenReturn("CUSTOM");
        when(scoped.getId()).thenReturn(2L);
        when(repository.findByIdentifier("scoped")).thenReturn(Optional.of(scoped));
        when(finder.findWorkspaceActiveForUpdate("scoped", 9L)).thenReturn(scoped);
        assertEquals(2L, adapter.resolveActiveForUpdate(9L, "scoped"));
        assertThrows(NotFoundException.class, () -> adapter.resolveActiveForUpdate(10L, "scoped"));
        assertThrows(NotFoundException.class, () -> adapter.resolveActiveForUpdate(9L, "missing"));

        Environment globalCustom = mock(Environment.class);
        EnvironmentType customGlobalType = mock(EnvironmentType.class);
        when(globalCustom.getWorkspaceId()).thenReturn(null);
        when(globalCustom.getEnvironmentType()).thenReturn(customGlobalType);
        when(customGlobalType.getCode()).thenReturn("CUSTOM");
        when(repository.findByIdentifier("global-custom")).thenReturn(Optional.of(globalCustom));
        assertThrows(NotFoundException.class, () -> adapter.resolveActiveForUpdate(9L, "global-custom"));
    }

    @Test
    void resolvesPersistedIdentifierIncludingInactiveReferences() {
        IdentifierReference reference = mock(IdentifierReference.class);
        when(reference.getIdentifier()).thenReturn("environment-id");
        when(repository.findIdentifierReferences(List.of(5L))).thenReturn(List.of(reference));
        assertEquals("environment-id", adapter.resolveIdentifier(5L));
        when(repository.findIdentifierReferences(List.of(6L))).thenReturn(List.of());
        assertThrows(NotFoundException.class, () -> adapter.resolveIdentifier(6L));
    }
}
