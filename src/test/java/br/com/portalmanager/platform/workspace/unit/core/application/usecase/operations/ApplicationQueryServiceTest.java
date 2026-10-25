package br.com.portalmanager.platform.workspace.unit.core.application.usecase.operations;


import br.com.portalmanager.platform.workspace.core.application.usecase.operations.ApplicationFinder;
import br.com.portalmanager.platform.workspace.core.application.usecase.operations.ApplicationNormalizer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.tagging.TagManager;
import br.com.portalmanager.platform.workspace.core.application.domain.Application;
import br.com.portalmanager.platform.workspace.core.application.domain.ApplicationTag;
import br.com.portalmanager.platform.workspace.core.application.repository.ApplicationRepository;
import br.com.portalmanager.platform.workspace.core.application.repository.projection.ApplicationReference;
import br.com.portalmanager.platform.workspace.core.application.repository.projection.ApplicationScopeReference;
import br.com.portalmanager.platform.workspace.core.application.usecase.mapping.ApplicationMapper;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationReferenceOutput;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationScopeReferenceOutput;
import br.com.portalmanager.platform.workspace.core.application.usecase.operations.ApplicationQueryService;
import br.com.portalmanager.platform.workspace.core.application.usecase.port.WorkspaceReferencePort;
import br.com.portalmanager.platform.workspace.core.application.usecase.validation.ApplicationValidator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ApplicationQueryServiceTest {

    private static final Long WORKSPACE_ID = 11L;
    private static final Long APPLICATION_ID = 23L;

    private final ApplicationRepository repository = mock(ApplicationRepository.class);
    private final ApplicationFinder finder = mock(ApplicationFinder.class);
    private final WorkspaceReferencePort workspaces = mock(WorkspaceReferencePort.class);
    @SuppressWarnings("unchecked")
    private final TagManager<ApplicationTag, Application> tags = mock(TagManager.class);
    private final ApplicationNormalizer normalizer = mock(ApplicationNormalizer.class);
    private final ApplicationValidator validator = mock(ApplicationValidator.class);
    private final ApplicationMapper mapper = mock(ApplicationMapper.class);

    private final ApplicationQueryService service =
            new ApplicationQueryService(repository, finder, workspaces, tags, normalizer, validator, mapper);

    @Test
    void resolvesActiveScopeForApplicationAndUpdate() {
        Application application = mock(Application.class);
        ApplicationScopeReferenceOutput expected =
                new ApplicationScopeReferenceOutput(WORKSPACE_ID, APPLICATION_ID, "BACKEND");
        when(workspaces.resolveInternalId("workspace")).thenReturn(WORKSPACE_ID);
        when(finder.findActive("application", WORKSPACE_ID)).thenReturn(application);
        when(finder.findActiveForUpdate("application", WORKSPACE_ID)).thenReturn(application);
        when(mapper.toScopeReference(WORKSPACE_ID, application)).thenReturn(expected);

        assertEquals(expected, service.findActiveScope("workspace", "application"));
        assertEquals(expected, service.findActiveScopeForUpdate("workspace", "application"));
    }

    @Test
    void sharedScopeRequiresActiveApplicationAndWorkspace() {
        ApplicationScopeReference scope = mock(ApplicationScopeReference.class);
        when(repository.findActiveScope(WORKSPACE_ID, APPLICATION_ID)).thenReturn(Optional.of(scope));
        when(workspaces.findActiveIdentifiersForResourceSharing(List.of(WORKSPACE_ID)))
                .thenReturn(Map.of(WORKSPACE_ID, "workspace"));
        ApplicationScopeReferenceOutput expected =
                new ApplicationScopeReferenceOutput(WORKSPACE_ID, APPLICATION_ID, "SHARED");
        when(mapper.toScopeReference(WORKSPACE_ID, APPLICATION_ID, scope)).thenReturn(expected);

        assertEquals(expected, service.findActiveScopeForShared(WORKSPACE_ID, APPLICATION_ID));

        when(workspaces.findActiveIdentifiersForResourceSharing(List.of(WORKSPACE_ID))).thenReturn(Map.of());
        assertThrows(
                NotFoundException.class,
                () -> service.findActiveScopeForShared(WORKSPACE_ID, APPLICATION_ID));

        when(repository.findActiveScope(WORKSPACE_ID, APPLICATION_ID)).thenReturn(Optional.empty());
        assertThrows(
                NotFoundException.class,
                () -> service.findActiveScopeForShared(WORKSPACE_ID, APPLICATION_ID));
    }

    @Test
    void findsIdentifierByInternalIdsOrReportsNotFound() {
        when(repository.findIdentifier(WORKSPACE_ID, APPLICATION_ID)).thenReturn(Optional.of("application"));
        assertEquals("application", service.findIdentifierByInternalId(WORKSPACE_ID, APPLICATION_ID));

        when(repository.findIdentifier(WORKSPACE_ID, APPLICATION_ID)).thenReturn(Optional.empty());
        assertThrows(
                NotFoundException.class,
                () -> service.findIdentifierByInternalId(WORKSPACE_ID, APPLICATION_ID));
    }

    @Test
    void returnsNoReferencesWithoutQueryingForEmptyIds() {
        assertEquals(Map.of(), service.findReferencesForResourceSharing(List.of()));
        assertEquals(List.of(), service.findActiveReferencesForResourceSharing(List.of()));
        verifyNoInteractions(repository, workspaces, mapper);
    }

    @Test
    void mapsReferencesIncludingInactiveApplicationsAndWorkspaces() {
        ApplicationReference reference = reference(APPLICATION_ID, WORKSPACE_ID, "application");
        ApplicationReferenceOutput expected =
                new ApplicationReferenceOutput(APPLICATION_ID, WORKSPACE_ID, "application", "workspace", "Application");
        when(repository.findReferences(List.of(APPLICATION_ID), null)).thenReturn(List.of(reference));
        when(workspaces.findReferenceIdentifiersForResourceSharing(List.of(WORKSPACE_ID)))
                .thenReturn(Map.of(WORKSPACE_ID, "workspace"));
        when(mapper.toReference(reference, "workspace")).thenReturn(expected);

        assertEquals(
                Map.of(APPLICATION_ID, expected),
                service.findReferencesForResourceSharing(List.of(APPLICATION_ID)));
    }

    @Test
    void mapsOnlyApplicationsWithActiveWorkspacesForDiscovery() {
        ApplicationReference active = reference(APPLICATION_ID, WORKSPACE_ID, "active-application");
        ApplicationReference unavailable = reference(24L, 12L, "unavailable-application");
        ApplicationReferenceOutput expected =
                new ApplicationReferenceOutput(APPLICATION_ID, WORKSPACE_ID, "active-application", "workspace", "Application");
        when(repository.findReferences(List.of(APPLICATION_ID, 24L), "ACTIVE"))
                .thenReturn(List.of(active, unavailable));
        when(workspaces.findActiveIdentifiersForResourceSharing(List.of(WORKSPACE_ID, 12L)))
                .thenReturn(Map.of(WORKSPACE_ID, "workspace"));
        when(mapper.toReference(active, "workspace")).thenReturn(expected);

        assertEquals(
                List.of(expected),
                service.findActiveReferencesForResourceSharing(List.of(APPLICATION_ID, 24L)));
        verify(mapper).toReference(active, "workspace");
    }

    private static ApplicationReference reference(Long id, Long workspaceId, String identifier) {
        ApplicationReference reference = mock(ApplicationReference.class);
        when(reference.getId()).thenReturn(id);
        when(reference.getWorkspaceId()).thenReturn(workspaceId);
        when(reference.getIdentifier()).thenReturn(identifier);
        when(reference.getName()).thenReturn("Application");
        return reference;
    }
}
