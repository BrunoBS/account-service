package br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.core.environment.domain.environment.Environment;
import br.com.portalmanager.platform.workspace.core.environment.repository.environment.EnvironmentRepository;
import br.com.portalmanager.platform.workspace.core.environment.repository.environment.EnvironmentRepository.HierarchyReference;
import br.com.portalmanager.platform.workspace.core.environment.repository.environment.EnvironmentRepository.IdentifierReference;
import br.com.portalmanager.platform.workspace.core.environment.usecase.mapping.environment.EnvironmentMapper;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environment.EnvironmentOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environment.EnvironmentReferenceOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environment.EnvironmentTreeOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment.EnvironmentFinder;
import br.com.portalmanager.platform.workspace.core.environment.usecase.port.WorkspaceReferencePort;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EnvironmentQueryServiceTest {
    private final EnvironmentRepository repository=mock(EnvironmentRepository.class);
    private final EnvironmentFinder finder=mock(EnvironmentFinder.class);
    private final WorkspaceReferencePort workspaces=mock(WorkspaceReferencePort.class);
    private final EnvironmentMapper mapper=mock(EnvironmentMapper.class);
    private final EnvironmentQueryService service=new EnvironmentQueryService(repository,finder,workspaces,mapper);
    private HierarchyReference hierarchy(Long id,Long parent,String lifecycle){
        var value=mock(HierarchyReference.class); when(value.getId()).thenReturn(id); when(value.getParentId()).thenReturn(parent);
        when(value.getLifecycle()).thenReturn(lifecycle); return value;
    }

    @Test void resolvesIdentifiersAndSharedReferencesSafely(){
        assertEquals(Map.of(),service.findIdentifiersByInternalIds(List.of()));
        assertEquals(Map.of(),service.findAvailableReferencesForResourceSharing("workspace",List.of()));
        IdentifierReference identifier=mock(IdentifierReference.class);
        when(identifier.getId()).thenReturn(2L); when(identifier.getIdentifier()).thenReturn("env-2");
        when(repository.findIdentifierReferences(List.of(2L))).thenReturn(List.of(identifier));
        assertEquals(Map.of(2L,"env-2"),service.findIdentifiersByInternalIds(List.of(2L)));

        when(workspaces.resolveInternalId("workspace")).thenReturn(5L);
        var activeReference=hierarchy(2L,null,"ACTIVE");
        when(repository.findHierarchyForSharedReferences(5L,List.of("env-2"))).thenReturn(List.of(activeReference));
        Environment environment=mock(Environment.class); when(environment.getId()).thenReturn(2L); when(environment.getIdentifier()).thenReturn("env-2");
        when(environment.getWorkspaceId()).thenReturn(5L); when(repository.findSharedReferences(5L,List.of("env-2"))).thenReturn(List.of(environment));
        var output=mock(EnvironmentOutput.class); var reference=mock(EnvironmentReferenceOutput.class);
        when(mapper.toOutput(environment,"workspace")).thenReturn(output); when(mapper.toReference(2L,output)).thenReturn(reference);
        assertEquals(Map.of("env-2",reference),service.findAvailableReferencesForResourceSharing("workspace",List.of("env-2")));
        when(repository.findSharedReferences(5L,List.of("missing"))).thenReturn(List.of());
        when(repository.findHierarchyForSharedReferences(5L,List.of("missing"))).thenReturn(List.of());
        assertThrows(NotFoundException.class,()->service.findAvailableReferencesForResourceSharing("workspace",List.of("missing")));
    }

    @Test void listsSharedEnvironmentsOnlyWhenEveryAncestorIsActive(){
        Environment root=mock(Environment.class); Environment child=mock(Environment.class); Environment hidden=mock(Environment.class);
        when(root.getId()).thenReturn(1L); when(root.getWorkspaceId()).thenReturn(null); when(root.getParent()).thenReturn(null);
        when(child.getId()).thenReturn(2L); when(child.getWorkspaceId()).thenReturn(5L); when(child.getParent()).thenReturn(root);
        when(hidden.getId()).thenReturn(3L); when(hidden.getWorkspaceId()).thenReturn(5L); when(hidden.getParent()).thenReturn(root);
        when(root.getLifecycle()).thenReturn(LifecycleTypeCode.active()); when(child.getLifecycle()).thenReturn(LifecycleTypeCode.active());
        when(hidden.getLifecycle()).thenReturn(LifecycleTypeCode.inactive());
        when(repository.findVisibleInWorkspace(5L,LifecycleTypeCode.active())).thenReturn(List.of(child,hidden,root));
        when(workspaces.resolveInternalId("workspace")).thenReturn(5L);
        when(mapper.toOutput(root,null)).thenReturn(mock(EnvironmentOutput.class));
        when(mapper.toOutput(child,"workspace")).thenReturn(mock(EnvironmentOutput.class));
        when(mapper.toOutput(hidden,"workspace")).thenReturn(mock(EnvironmentOutput.class));
        assertEquals(2,service.listActiveForShared("workspace").size());
    }

    @Test void resolvesWorkspaceOrDefaultSharedEnvironmentAndChecksVisibility(){
        Environment environment=mock(Environment.class); when(environment.getWorkspaceId()).thenReturn(null);
        when(workspaces.resolveInternalId("workspace")).thenReturn(5L);
        when(repository.findByIdentifierAndWorkspaceId("global",5L)).thenReturn(Optional.empty());
        when(repository.findByIdentifierAndWorkspaceIdIsNull("global")).thenReturn(Optional.of(environment));
        when(finder.isAccessible(environment)).thenReturn(true);
        var output=mock(EnvironmentOutput.class); when(mapper.toOutput(environment,null)).thenReturn(output);
        assertSame(output,service.findAvailableForShared("workspace","global"));
        when(finder.isAccessible(environment)).thenReturn(false);
        assertThrows(NotFoundException.class,()->service.findAvailableForShared("workspace","global"));
    }

    @Test void findsRootsChildrenAndBuildsTheVisibleTree(){
        when(workspaces.resolveInternalId("workspace")).thenReturn(5L);
        Environment global=mock(Environment.class); Environment root=mock(Environment.class); Environment child=mock(Environment.class);
        when(global.getWorkspaceId()).thenReturn(null); when(root.getWorkspaceId()).thenReturn(5L);
        when(root.getParent()).thenReturn(null); when(child.getParent()).thenReturn(root);
        when(root.getId()).thenReturn(1L); when(child.getId()).thenReturn(2L);
        when(repository.findDefaultRoots(LifecycleTypeCode.active())).thenReturn(List.of(global));
        when(repository.findWorkspaceRoots(5L,LifecycleTypeCode.active())).thenReturn(List.of(root));
        var globalOutput=mock(EnvironmentOutput.class); var rootOutput=mock(EnvironmentOutput.class);
        when(mapper.toOutput(global,null)).thenReturn(globalOutput);
        when(mapper.toOutput(root,"workspace")).thenReturn(rootOutput);
        assertEquals(2,service.findRoots("workspace").size());

        when(repository.findByIdentifier("parent")).thenReturn(Optional.of(root));
        when(finder.isAccessible(root)).thenReturn(true);
        when(repository.findChildren(1L,5L,LifecycleTypeCode.active())).thenReturn(List.of(child));
        var childOutput=mock(EnvironmentOutput.class);
        when(mapper.toOutput(child,"workspace")).thenReturn(childOutput);
        assertEquals(1,service.findChildren("workspace","parent").size());
        when(repository.findByIdentifier("outside")).thenReturn(Optional.of(global));
        assertThrows(NotFoundException.class,()->service.findChildren("workspace","outside"));
        when(finder.isAccessible(root)).thenReturn(false);
        assertThrows(NotFoundException.class,()->service.findChildren("workspace","parent"));

        when(root.getParent()).thenReturn(null); when(root.getId()).thenReturn(1L);
        when(child.getParent()).thenReturn(root); when(child.getId()).thenReturn(2L);
        when(repository.findVisibleInWorkspace(5L,LifecycleTypeCode.active())).thenReturn(List.of(root,child));
        when(mapper.toTreeOutput(any(EnvironmentOutput.class),anyList())).thenAnswer(invocation ->
                new EnvironmentTreeOutput(invocation.getArgument(0),invocation.getArgument(1)));
        List<EnvironmentTreeOutput> tree=service.findTree("workspace");
        assertEquals(1,tree.size()); assertEquals(1,tree.getFirst().children().size());
    }

    @Test void mapsWorkspaceAndGlobalInactiveReads(){
        when(workspaces.resolveInternalId("workspace")).thenReturn(5L);
        var entity=mock(Environment.class); var output=mock(EnvironmentOutput.class);
        when(finder.findInactive("inactive",5L)).thenReturn(entity);
        when(finder.findInactiveForDeletion("deleted",5L)).thenReturn(entity);
        when(finder.findInactive("global",null)).thenReturn(entity);
        when(finder.findInactiveForDeletion("global-deleted",null)).thenReturn(entity);
        when(mapper.toOutput(entity,"workspace")).thenReturn(output); when(mapper.toOutput(entity,null)).thenReturn(output);
        assertSame(output,service.findWorkspaceInactive("workspace","inactive"));
        assertSame(output,service.findWorkspaceForDeletion("workspace","deleted"));
        assertSame(output,service.findDefaultInactive("global"));
        assertSame(output,service.findDefaultForDeletion("global-deleted"));
    }

    @Test void filtersWorkspaceAndDefaultListsByVisibleAncestorLifecycle(){
        when(workspaces.resolveInternalId("workspace")).thenReturn(5L);
        var root=hierarchy(1L,null,"ACTIVE");
        var childRef=hierarchy(2L,1L,"ACTIVE");
        var orphanRef=hierarchy(3L,99L,"ACTIVE");
        when(repository.findVisibleHierarchyInWorkspace(5L)).thenReturn(List.of(root,childRef,orphanRef));
        Environment rootEnv=mock(Environment.class); Environment child=mock(Environment.class); Environment orphan=mock(Environment.class);
        when(rootEnv.getId()).thenReturn(1L); when(child.getId()).thenReturn(2L); when(orphan.getId()).thenReturn(3L);
        when(child.getParent()).thenReturn(rootEnv); when(orphan.getParent()).thenReturn(null);
        when(repository.findByWorkspaceAndLifecycle(5L,LifecycleTypeCode.active())).thenReturn(List.of(child,orphan));
        IdentifierReference parentIdentifier=mock(IdentifierReference.class);
        when(parentIdentifier.getId()).thenReturn(1L); when(parentIdentifier.getIdentifier()).thenReturn("root");
        IdentifierReference inactiveParentIdentifier=mock(IdentifierReference.class);
        when(inactiveParentIdentifier.getId()).thenReturn(10L); when(inactiveParentIdentifier.getIdentifier()).thenReturn("inactive-root");
        when(repository.findIdentifierReferences(anyCollection())).thenAnswer(invocation ->
                ((java.util.Collection<Long>) invocation.getArgument(0)).contains(1L)
                        ? List.of(parentIdentifier) : List.of(inactiveParentIdentifier));
        when(mapper.toOutput(child,"workspace","root")).thenReturn(mock(EnvironmentOutput.class));
        assertEquals(1,service.listWorkspace("workspace","ACTIVE").size());

        var inactiveRoot=hierarchy(10L,null,"INACTIVE");
        var inactiveChild=hierarchy(11L,10L,"INACTIVE");
        Environment inactiveRootEnv=mock(Environment.class); Environment inactiveChildEnv=mock(Environment.class);
        when(inactiveRootEnv.getId()).thenReturn(10L); when(inactiveChildEnv.getId()).thenReturn(11L);
        when(inactiveRootEnv.getParent()).thenReturn(null); when(inactiveChildEnv.getParent()).thenReturn(inactiveRootEnv);
        when(repository.findDefaultsByLifecycle(LifecycleTypeCode.inactive())).thenReturn(List.of(inactiveRootEnv,inactiveChildEnv));
        when(mapper.toOutput(inactiveRootEnv,null,null)).thenReturn(mock(EnvironmentOutput.class));
        when(mapper.toOutput(inactiveChildEnv,null,"inactive-root")).thenReturn(mock(EnvironmentOutput.class));
        assertEquals(2,service.listDefaults("INACTIVE").size());
        when(repository.findDefaultsByLifecycle(LifecycleTypeCode.active())).thenReturn(List.of());
        assertTrue(service.listDefaults("ACTIVE").isEmpty());
    }

    @Test void rejectsListResultsWhenAParentIdentifierCannotBeResolved(){
        Environment parent=mock(Environment.class); Environment child=mock(Environment.class);
        when(child.getParent()).thenReturn(parent); when(parent.getId()).thenReturn(7L);
        when(repository.findDefaultsByLifecycle(LifecycleTypeCode.inactive())).thenReturn(List.of(child));
        when(repository.findIdentifierReferences(List.of(7L))).thenReturn(List.of());
        assertThrows(NotFoundException.class,()->service.listDefaults("INACTIVE"));
    }
}
