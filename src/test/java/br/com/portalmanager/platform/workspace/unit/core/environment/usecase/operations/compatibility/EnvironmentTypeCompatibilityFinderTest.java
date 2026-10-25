package br.com.portalmanager.platform.workspace.unit.core.environment.usecase.operations.compatibility;


import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.compatibility.EnvironmentTypeCompatibilityFinder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.core.environment.domain.compatibility.EnvironmentTypeCompatibility;
import br.com.portalmanager.platform.workspace.core.environment.repository.compatibility.EnvironmentTypeCompatibilityRepository;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EnvironmentTypeCompatibilityFinderTest {
    private final EnvironmentTypeCompatibilityRepository repository=mock(EnvironmentTypeCompatibilityRepository.class);
    private final EntityManager entityManager=mock(EntityManager.class);
    private final EnvironmentTypeCompatibilityFinder finder=new EnvironmentTypeCompatibilityFinder(repository,entityManager);

    @Test void locksAndReloadsCompatibilityByIdentifierOrPair(){
        var entity=mock(EnvironmentTypeCompatibility.class); var reloaded=mock(EnvironmentTypeCompatibility.class);
        when(repository.findByIdentifierForUpdate("id")).thenReturn(Optional.of(entity),Optional.of(reloaded));
        assertSame(reloaded,finder.findForUpdate("id")); verify(entityManager).detach(entity);
        when(repository.findByTypePairForUpdate("CUSTOM","SHARD")).thenReturn(Optional.of(entity),Optional.of(reloaded));
        assertSame(reloaded,finder.findByTypePairForUpdate("CUSTOM","SHARD").orElseThrow());
        verify(entityManager,times(2)).detach(entity);
        when(repository.findByIdentifierForUpdate("missing")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class,()->finder.findForUpdate("missing"));
        when(repository.findByTypePairForUpdate("MISSING","PAIR")).thenReturn(Optional.empty());
        assertTrue(finder.findByTypePairForUpdate("MISSING","PAIR").isEmpty());
    }
}
