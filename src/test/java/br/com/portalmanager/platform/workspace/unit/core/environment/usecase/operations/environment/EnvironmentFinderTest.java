package br.com.portalmanager.platform.workspace.unit.core.environment.usecase.operations.environment;


import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment.EnvironmentFinder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.core.environment.domain.environment.Environment;
import br.com.portalmanager.platform.workspace.core.environment.repository.environment.EnvironmentRepository;
import br.com.portalmanager.platform.workspace.core.environment.repository.environment.EnvironmentRepository.HierarchyReference;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EnvironmentFinderTest {
    private final EnvironmentRepository repository=mock(EnvironmentRepository.class);
    private final EntityManager entityManager=mock(EntityManager.class);
    private final EnvironmentFinder finder=new EnvironmentFinder(repository,entityManager);
    private Environment env(Long workspace,Environment parent,String lifecycle){
        var value=mock(Environment.class); when(value.getWorkspaceId()).thenReturn(workspace); when(value.getParent()).thenReturn(parent);
        when(value.getLifecycle()).thenReturn("ACTIVE".equals(lifecycle)?LifecycleTypeCode.active():LifecycleTypeCode.inactive());
        return value;
    }

    @Test void findsActiveAndInactiveResourcesWithinTheirWorkspace(){
        var active=env(5L,null,"ACTIVE"); var inactive=env(5L,null,"INACTIVE");
        when(repository.findByIdentifierAndWorkspaceId("active",5L)).thenReturn(Optional.of(active));
        when(repository.findByIdentifierAndWorkspaceId("inactive",5L)).thenReturn(Optional.of(inactive));
        assertSame(active,finder.findActive("active",5L));
        assertSame(inactive,finder.findInactive("inactive",5L));
        assertSame(inactive,finder.findInactiveForDeletion("inactive",5L));
        var inaccessible=env(5L,null,"INACTIVE");
        assertThrows(NotFoundException.class,()->finder.findActive("inactive",5L));
        assertThrows(NotFoundException.class,()->finder.findActive("missing",5L));
        verify(inaccessible,never()).getIdentifier();
    }

    @Test void locksAndRevalidatesWorkspaceRowsAndAncestors(){
        var parent=mock(Environment.class); when(parent.getId()).thenReturn(10L);
        var snapshot=env(5L,parent,"ACTIVE"); var locked=env(5L,parent,"ACTIVE");
        when(repository.findByIdentifierAndWorkspaceId("item",5L)).thenReturn(Optional.of(snapshot));
        var hierarchy=mock(HierarchyReference.class); when(hierarchy.getParentId()).thenReturn(null);
        when(repository.findHierarchyReference(10L)).thenReturn(Optional.of(hierarchy));
        var visibleParent=mock(Environment.class);
        when(repository.findVisibleByIdAndLifecycleForRead(10L,LifecycleTypeCode.active())).thenReturn(Optional.of(visibleParent));
        when(repository.findByIdentifierForUpdate("item")).thenReturn(Optional.of(locked));
        assertSame(locked,finder.findWorkspaceActiveForUpdate("item",5L));
        verify(entityManager).detach(snapshot);

        var inactive=env(null,null,"INACTIVE");
        when(repository.findByIdentifierForUpdate("global")).thenReturn(Optional.of(inactive));
        when(repository.findByIdentifierAndWorkspaceIdIsNull("global")).thenReturn(Optional.of(inactive));
        assertSame(inactive,finder.findInactiveForRestore("global",null));
        assertSame(inactive,finder.findInactiveForDeletionForUpdate("global",null));
        assertThrows(NotFoundException.class,()->finder.findActiveForUpdate("global",null));
    }

    @Test void rejectsMissingOrInvisibleAncestorsAndResolvesParentType(){
        var parent=mock(Environment.class); when(parent.getId()).thenReturn(10L);
        var child=env(5L,parent,"ACTIVE");
        when(repository.findByIdentifierAndWorkspaceId("child",5L)).thenReturn(Optional.of(child));
        when(repository.findVisibleById(10L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class,()->finder.findActive("child",5L));
        assertThrows(NotFoundException.class,()->finder.findInactive("child",5L));
        var reference=mock(HierarchyReference.class); when(reference.getTypeCode()).thenReturn("CUSTOM");
        when(repository.findHierarchyReferenceForRead(10L)).thenReturn(Optional.of(reference));
        assertEquals("CUSTOM",finder.findParentTypeCodeForValidation(child));
        when(repository.findHierarchyReferenceForRead(10L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class,()->finder.findParentTypeCodeForValidation(child));
    }
}
