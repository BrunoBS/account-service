package br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environmenttype;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentType;
import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentTypeDetails;
import br.com.portalmanager.platform.workspace.core.environment.repository.environmenttype.EnvironmentTypeRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EnvironmentTypeFinderTest {
    private final EnvironmentTypeRepository repository=mock(EnvironmentTypeRepository.class);
    private final EntityManager entityManager=mock(EntityManager.class);
    private final EnvironmentTypeFinder finder=new EnvironmentTypeFinder(repository,entityManager);
    private EnvironmentType type(String code){return new EnvironmentType(code,new EnvironmentTypeDetails(code,"Type",true,true,!"DEFAULT".equals(code),1),LocalDateTime.now());}

    @Test void findsAndLocksTypesForEnvironmentOperations(){
        var custom=type("CUSTOM"); var parent=type("DEFAULT");
        when(entityManager.getReference(eq(EnvironmentType.class),anyString())).thenAnswer(invocation->type(invocation.getArgument(1)));
        when(repository.findByCodeForRead("CUSTOM")).thenReturn(Optional.of(custom));
        when(repository.findByCodeForRead("DEFAULT")).thenReturn(Optional.of(parent));
        assertSame(custom,finder.findForEnvironmentCreation("CUSTOM",true,null));
        assertSame(custom,finder.findForEnvironmentUpdate("CUSTOM","CUSTOM",null));
        assertSame(custom,finder.findForEnvironmentUpdate("DEFAULT","CUSTOM","DEFAULT"));
        assertSame(custom,finder.findForEnvironmentRestoration("CUSTOM","DEFAULT"));
        assertThrows(NotFoundException.class,()->finder.findForEnvironmentCreation("CUSTOM",false,null));
        assertThrows(NotFoundException.class,()->finder.findForEnvironmentUpdate("CUSTOM"," ",null));
        custom.inactivate(LocalDateTime.now());
        when(repository.findByCodeForRead("CUSTOM")).thenReturn(Optional.of(custom));
        assertThrows(NotFoundException.class,()->finder.findForEnvironmentCreation("CUSTOM",true,null));
        custom.restore(LocalDateTime.now());
        assertThrows(NotFoundException.class,()->finder.findForEnvironmentUpdate("CUSTOM","CUSTOM"," "));
        when(repository.findById("CUSTOM")).thenReturn(Optional.of(custom));
        assertSame(custom,finder.find("CUSTOM"));
        when(repository.findByCodeForUpdate("CUSTOM")).thenReturn(Optional.of(custom));
        assertSame(custom,finder.findForUpdate("CUSTOM"));
        when(repository.findAllCodes()).thenReturn(List.of("CUSTOM","DEFAULT"));
        when(repository.findAllForUpdate()).thenReturn(List.of(custom,parent));
        assertEquals(2,finder.findAllForUpdate().size());
        assertSame(custom,finder.findForConfigurationUpdate("CUSTOM"));
        assertThrows(NotFoundException.class,()->finder.findForConfigurationUpdate("MISSING"));
    }
}
