package br.com.portalmanager.platform.workspace.unit.core.environment.usecase.validation.environment;


import br.com.portalmanager.platform.workspace.core.environment.usecase.validation.environment.EnvironmentValidator;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.core.environment.domain.environment.Environment;
import br.com.portalmanager.platform.workspace.core.environment.repository.environment.EnvironmentRepository;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environment.CreateEnvironmentInput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environment.UpdateEnvironmentInput;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EnvironmentValidatorTest {
    private final EnvironmentRepository repository=mock(EnvironmentRepository.class);
    private final EnvironmentValidator validator=new EnvironmentValidator(repository);
    private CreateEnvironmentInput create(Long version,String type){return new CreateEnvironmentInput(version,"Env","Description","DEV",null,null,null,type,null);}
    private UpdateEnvironmentInput update(Long version,String type,String parent){return new UpdateEnvironmentInput(version,"Env","Description","DEV",null,null,null,type,parent);}

    @Test void validatesCreateUpdateAndSiblingNames(){
        validator.validateForCreate(create(null,"CUSTOM"));
        assertThrows(ValidationException.class,()->validator.validateForCreate(null));
        assertThrows(ValidationException.class,()->validator.validateForCreate(create(1L,"CUSTOM")));
        assertThrows(ValidationException.class,()->validator.validateForCreate(create(null," ")));
        assertThrows(ValidationException.class,()->validator.validateForCreate(new CreateEnvironmentInput(null,"Env","Description","DEV",null,null,null,null,null)));
        validator.validateForUpdate(update(0L,"CUSTOM",null));
        assertThrows(ValidationException.class,()->validator.validateForUpdate(null));
        assertThrows(ValidationException.class,()->validator.validateForUpdate(update(null,"CUSTOM",null)));
        assertThrows(ValidationException.class,()->validator.validateForUpdate(update(0L," ",null)));
        assertThrows(ValidationException.class,()->validator.validateForUpdate(new UpdateEnvironmentInput(0L,"Env","Description","DEV",null,null,null,null,null)));
        assertThrows(ValidationException.class,()->validator.validateForUpdate(update(0L,"CUSTOM","parent")));
        when(repository.existsSibling("Unique",10L,20L,30L)).thenReturn(false);
        validator.validateNameAvailable("Unique",10L,20L,30L);
        when(repository.existsSibling("Taken",10L,20L,30L)).thenReturn(true);
        assertThrows(ValidationException.class,()->validator.validateNameAvailable("Taken",10L,20L,30L));
        validator.requireVersion(2L,2L);
        assertThrows(ResourceVersionConflictException.class,()->validator.requireVersion(2L,1L));
        validator.validateNameAvailable(null,10L,20L,30L);
        verify(repository,never()).existsSibling(null,10L,20L,30L);
    }

    @Test void deleteRequiresInactiveEnvironmentWithoutChildren(){
        Environment environment=mock(Environment.class);
        when(environment.getId()).thenReturn(10L);
        doNothing().when(environment).requireDeletable();
        when(repository.findChildForEnvironmentValidation(10L)).thenReturn(Optional.empty());
        validator.validateForDelete(environment);
        when(repository.findChildForEnvironmentValidation(10L)).thenReturn(Optional.of(11L));
        assertThrows(ValidationException.class,()->validator.validateForDelete(environment));
    }
}
