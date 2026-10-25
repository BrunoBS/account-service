package br.com.portalmanager.platform.workspace.unit.core.environment.usecase.validation.environment;


import br.com.portalmanager.platform.workspace.core.environment.usecase.validation.environment.EnvironmentTopologyValidator;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.core.environment.domain.environment.Environment;
import br.com.portalmanager.platform.workspace.core.environment.domain.environment.EnvironmentDetails;
import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentType;
import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentTypeDetails;
import br.com.portalmanager.platform.workspace.core.environment.repository.compatibility.EnvironmentTypeCompatibilityRepository;
import br.com.portalmanager.platform.workspace.core.environment.repository.environment.EnvironmentRepository;
import br.com.portalmanager.platform.workspace.core.environment.repository.environmenttype.EnvironmentTypeRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class EnvironmentTopologyValidatorTest {
    private final EnvironmentTypeCompatibilityRepository compatibilities=mock(EnvironmentTypeCompatibilityRepository.class);
    private final EnvironmentTypeRepository types=mock(EnvironmentTypeRepository.class);
    private final EnvironmentRepository environments=mock(EnvironmentRepository.class);
    private final EnvironmentTopologyValidator validator=new EnvironmentTopologyValidator(compatibilities,types,environments);
    private EnvironmentType type(String code,boolean root,boolean children,boolean workspace){return new EnvironmentType(code,new EnvironmentTypeDetails(code,"Type",root,children,workspace,1),LocalDateTime.now());}
    private Environment env(Long workspace,EnvironmentType type){return new Environment(workspace,type,null,new EnvironmentDetails("Parent","Parent description",br.com.portalmanager.platform.workspace.foundation.catalog.authorizationtype.domain.AuthorizationTypeCode.development(),null,JsonMapper.builder().build().createObjectNode(),1),LocalDateTime.now());}

    @Test void validatesRootScopeAndNewOrExistingLinks(){
        var global=type("DEFAULT",true,true,false); var custom=type("CUSTOM",true,true,true); var shard=type("SHARD",false,true,true);
        validator.validateForNewLink(global,null,null);
        validator.validateForNewLink(custom,5L,null);
        assertThrows(ValidationException.class,()->validator.validateForNewLink(custom,null,null));
        assertThrows(ValidationException.class,()->validator.validateForNewLink(shard,5L,null));
        var parent=env(5L,custom);
        when(types.findByCodeForRead("CUSTOM")).thenReturn(Optional.of(custom));
        when(compatibilities.findActiveLinkForValidation("CUSTOM","SHARD")).thenReturn(Optional.of(1L));
        validator.validateForNewLink(shard,5L,parent);
        validator.validateForExistingLink(shard,5L,parent);
        when(compatibilities.findActiveLinkForValidation("CUSTOM","SHARD")).thenReturn(Optional.empty());
        assertThrows(ValidationException.class,()->validator.validateForNewLink(shard,5L,parent));
        assertThrows(ValidationException.class,()->validator.validateForNewLink(shard,6L,parent));
        when(types.findByCodeForRead("CUSTOM")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class,()->validator.validateForNewLink(shard,5L,parent));
        var leafParentType=type("CELL",false,false,true);
        var leafParent=env(5L,leafParentType);
        when(types.findByCodeForRead("CELL")).thenReturn(Optional.of(leafParentType));
        assertThrows(ValidationException.class,()->validator.validateForNewLink(shard,5L,leafParent));
    }

    @Test void validatesTypeChangesChildrenAndUnchangedParentType(){
        var currentType=type("CUSTOM",true,true,true); var newType=type("SHARD",false,true,true);
        var env=mock(Environment.class); when(env.getEnvironmentType()).thenReturn(currentType); when(env.getWorkspaceId()).thenReturn(5L);
        var parent=env(5L,currentType);
        when(env.getParent()).thenReturn(parent); when(env.getId()).thenReturn(10L);
        when(types.findByCodeForRead("CUSTOM")).thenReturn(Optional.of(currentType));
        when(compatibilities.findActiveLinkForValidation("CUSTOM","SHARD")).thenReturn(Optional.of(1L));
        validator.validateForUpdate(env,currentType);
        when(environments.findChildForEnvironmentValidation(10L)).thenReturn(Optional.empty());
        validator.validateForUpdate(env,newType);
        when(environments.findChildForEnvironmentValidation(10L)).thenReturn(Optional.of(11L));
        assertThrows(ValidationException.class,()->validator.validateForUpdate(env,newType));
        validator.validateParentTypeUnchanged("CUSTOM","CUSTOM");
        assertThrows(ValidationException.class,()->validator.validateParentTypeUnchanged("CUSTOM","SHARD"));
    }
}
