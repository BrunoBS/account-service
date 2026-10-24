package br.com.portalmanager.platform.workspace.core.environment.usecase.validation.environmenttype;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentType;
import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentTypeDetails;
import br.com.portalmanager.platform.workspace.core.environment.repository.compatibility.EnvironmentTypeCompatibilityRepository;
import br.com.portalmanager.platform.workspace.core.environment.repository.environment.EnvironmentRepository;
import br.com.portalmanager.platform.workspace.core.environment.repository.environmenttype.EnvironmentTypeRepository;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environmenttype.EnvironmentTypeInput;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class EnvironmentTypeValidatorTest {
    private final EnvironmentTypeRepository types=mock(EnvironmentTypeRepository.class);
    private final EnvironmentRepository environments=mock(EnvironmentRepository.class);
    private final EnvironmentTypeCompatibilityRepository compatibilities=mock(EnvironmentTypeCompatibilityRepository.class);
    private final EnvironmentTypeValidator validator=new EnvironmentTypeValidator(types,environments,compatibilities);
    private EnvironmentType type(String code,boolean root,boolean children,boolean workspace){
        var type=new EnvironmentType(code,new EnvironmentTypeDetails(code,"Type description",root,children,workspace,1),LocalDateTime.now());
        ReflectionTestUtils.setField(type,"version",0L); return type;
    }
    private EnvironmentTypeInput input(Long version,String code,boolean root,boolean children,boolean workspace){return new EnvironmentTypeInput(version,code,"Name","Description",root,children,workspace,1);}
    private EnvironmentTypeInput input(long version,String code,boolean root,boolean children,boolean workspace){return input(Long.valueOf(version),code,root,children,workspace);}

    @Test void validatesCreateScopeAndDuplicates(){
        validator.validateForCreate(new EnvironmentTypeInput(null,"DEFAULT","Default","Description",true,true,false,1));
        validator.validateForCreate(new EnvironmentTypeInput(null,"CUSTOM","Custom","Description",true,true,true,1));
        validator.validateForCreate(new EnvironmentTypeInput(null,"SHARD","Shard","Description",false,true,true,1));
        validator.validateForCreate(new EnvironmentTypeInput(null,"CELL","Cell","Description",false,false,true,1));
        assertThrows(ValidationException.class,()->validator.validateForCreate(null));
        assertThrows(ValidationException.class,()->validator.validateForCreate(new EnvironmentTypeInput(null,"DEFAULT","Name","Description",false,true,true,1)));
        when(types.existsByCode("CUSTOM")).thenReturn(true);
        assertThrows(ValidationException.class,()->validator.validateForCreate(new EnvironmentTypeInput(null,"CUSTOM","Name","Description",true,true,true,1)));
        assertThrows(ValidationException.class,()->validator.validateForCreate(new EnvironmentTypeInput(null,null,"Name","Description",true,true,true,1)));
        assertThrows(ValidationException.class,()->validator.validateForCreate(input(0,"DEFAULT",true,true,false)));
        assertThrows(ValidationException.class,()->validator.validateForCreate(input(null,"UNKNOWN",true,true,true)));
        assertThrows(ValidationException.class,()->validator.validateForCreate(input(null,"DEFAULT",true,true,true)));
        assertThrows(ValidationException.class,()->validator.validateForCreate(input(null,"CUSTOM",false,true,true)));
        assertThrows(ValidationException.class,()->validator.validateForCreate(input(null,"SHARD",false,true,false)));
    }

    @Test void validatesUpdatesVersionsImmutabilityAndChildren(){
        var cell=type("CELL",false,false,true);
        validator.validateForUpdate(cell,input(0,"CELL",false,false,true));
        assertThrows(ValidationException.class,()->validator.validateForUpdate(cell,null));
        assertThrows(ValidationException.class,()->validator.validateForUpdate(cell,new EnvironmentTypeInput(null,"CELL","Name","Description",false,false,true,1)));
        assertThrows(ValidationException.class,()->validator.validateForUpdate(cell,input(-1,"CELL",false,false,true)));
        assertThrows(ResourceVersionConflictException.class,()->validator.validateForUpdate(cell,input(2,"CELL",false,false,true)));
        assertThrows(ValidationException.class,()->validator.validateForUpdate(cell,input(0,"SHARD",false,false,true)));
        assertThrows(ValidationException.class,()->validator.validateForUpdate(cell,input(0,"CELL",false,false,false)));
        assertThrows(ValidationException.class,()->validator.validateForUpdate(cell,input(0,"CELL",true,false,true)));
        var parent=type("DEFAULT",true,true,false);
        when(compatibilities.findActiveChildLinkForTypeValidation("DEFAULT")).thenReturn(Optional.of(1L));
        assertThrows(ValidationException.class,()->validator.validateForUpdate(parent,input(0,"DEFAULT",true,false,false)));
        when(compatibilities.findActiveChildLinkForTypeValidation("DEFAULT")).thenReturn(Optional.empty());
        when(environments.findChildForTypeValidation("DEFAULT")).thenReturn(Optional.empty());
        validator.validateForUpdate(parent,input(0,"DEFAULT",true,true,false));
        when(environments.findUsageForTypeValidation("CUSTOM")).thenReturn(Optional.of(1L));
        var workspaceRequiredType=type("CUSTOM",true,true,true);
        assertThrows(ValidationException.class,()->validator.validateForUpdate(workspaceRequiredType,input(0,"CUSTOM",true,true,false)));
        when(environments.findUsageForTypeValidation("CUSTOM")).thenReturn(Optional.empty());
        assertThrows(ValidationException.class,()->validator.validateForUpdate(workspaceRequiredType,input(0,"CUSTOM",true,true,false)));
        validator.validateForUpdate(workspaceRequiredType,input(0,"CUSTOM",true,true,true));
        when(environments.findRootForTypeValidation("DEFAULT")).thenReturn(Optional.empty());
        assertThrows(ValidationException.class,()->validator.validateForUpdate(parent,input(0,"DEFAULT",false,true,false)));
        validator.validateForUpdate(parent,input(0,"DEFAULT",true,true,false));
        when(environments.findRootForTypeValidation("DEFAULT")).thenReturn(Optional.of(2L));
        assertThrows(ValidationException.class,()->validator.validateForUpdate(parent,input(0,"DEFAULT",false,true,false)));
        when(compatibilities.findActiveChildLinkForTypeValidation("DEFAULT")).thenReturn(Optional.empty());
        when(environments.findChildForTypeValidation("DEFAULT")).thenReturn(Optional.of(2L));
        assertThrows(ValidationException.class,()->validator.validateForUpdate(parent,input(0,"DEFAULT",true,false,false)));
    }

    @Test void preventsDeletionUntilInactiveAndUnused(){
        var cell=type("CELL",false,false,true);
        assertThrows(ValidationException.class,()->validator.validateForDelete(cell));
        cell.inactivate(LocalDateTime.now());
        when(environments.findUsageForTypeValidation("CELL")).thenReturn(Optional.empty());
        when(compatibilities.findUsageForTypeValidation("CELL")).thenReturn(Optional.empty());
        validator.validateForDelete(cell);
        when(compatibilities.findUsageForTypeValidation("CELL")).thenReturn(Optional.of(1L));
        assertThrows(ValidationException.class,()->validator.validateForDelete(cell));
        when(compatibilities.findUsageForTypeValidation("CELL")).thenReturn(Optional.empty());
        when(environments.findUsageForTypeValidation("CELL")).thenReturn(Optional.of(1L));
        assertThrows(ValidationException.class,()->validator.validateForDelete(cell));
    }
}
