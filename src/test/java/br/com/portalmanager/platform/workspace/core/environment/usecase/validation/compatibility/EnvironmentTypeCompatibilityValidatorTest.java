package br.com.portalmanager.platform.workspace.core.environment.usecase.validation.compatibility;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.core.environment.domain.compatibility.EnvironmentTypeCompatibility;
import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentType;
import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentTypeDetails;
import br.com.portalmanager.platform.workspace.core.environment.repository.compatibility.EnvironmentTypeCompatibilityRepository;
import br.com.portalmanager.platform.workspace.core.environment.repository.environment.EnvironmentRepository;
import br.com.portalmanager.platform.workspace.core.environment.usecase.validation.compatibility.EnvironmentTypeCompatibilityValidator.TypePair;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EnvironmentTypeCompatibilityValidatorTest {
    private final EnvironmentTypeCompatibilityRepository compatibilities=mock(EnvironmentTypeCompatibilityRepository.class);
    private final EnvironmentRepository environments=mock(EnvironmentRepository.class);
    private final EnvironmentTypeCompatibilityValidator validator=new EnvironmentTypeCompatibilityValidator(compatibilities,environments);
    private EnvironmentType type(String code,boolean children,boolean workspace){return new EnvironmentType(code,new EnvironmentTypeDetails(code,"Type",code.equals("CUSTOM")||code.equals("DEFAULT"),children,workspace,1),LocalDateTime.now());}
    private final EnvironmentType defaultType=type("DEFAULT",true,false);
    private final EnvironmentType customType=type("CUSTOM",true,true);
    private final EnvironmentType shardType=type("SHARD",true,true);
    private final EnvironmentType cellType=type("CELL",false,true);

    @Test void validatesKnownActivePairsScopeAndCycles(){
        assertEquals(new TypePair(customType,shardType),validator.validateForCreate(" CUSTOM ","SHARD",List.of(defaultType,customType,shardType,cellType)));
        assertThrows(ValidationException.class,()->validator.validateForCreate(null,"SHARD",List.of(shardType)));
        assertThrows(ValidationException.class,()->validator.validateForCreate("MISSING","SHARD",List.of(shardType)));
        assertThrows(ValidationException.class,()->validator.validateForCreate("CELL","SHARD",List.of(shardType,cellType)));
        assertThrows(ValidationException.class,()->validator.validateForCreate("CUSTOM","DEFAULT",List.of(customType,defaultType)));
        assertThrows(ValidationException.class,()->validator.validateForCreate("CUSTOM","CUSTOM",List.of(customType)));
        EnvironmentTypeCompatibilityRepository.TypeLink link=mock(EnvironmentTypeCompatibilityRepository.TypeLink.class);
        when(link.getParentTypeCode()).thenReturn("SHARD"); when(link.getChildTypeCode()).thenReturn("CELL");
        when(compatibilities.findActiveTypeLinksForValidation()).thenReturn(List.of(link));
        assertThrows(ValidationException.class,()->validator.validateForCreate("CELL","SHARD",List.of(shardType,cellType)));
    }

    @Test void rejectsInactiveTypesAndDetectsIndirectCycles(){
        var inactiveType=type("INACTIVE",true,true);
        inactiveType.inactivate(LocalDateTime.now());
        assertThrows(ValidationException.class,()->validator.validateForCreate("INACTIVE","SHARD",List.of(inactiveType,shardType)));
        assertThrows(ValidationException.class,()->validator.validateForCreate(" ","SHARD",List.of(shardType)));
        assertThrows(ValidationException.class,()->validator.validateForCreate("CUSTOM"," ",List.of(customType)));

        EnvironmentTypeCompatibilityRepository.TypeLink first=mock(EnvironmentTypeCompatibilityRepository.TypeLink.class);
        when(first.getParentTypeCode()).thenReturn("SHARD");
        when(first.getChildTypeCode()).thenReturn("CUSTOM");
        EnvironmentTypeCompatibilityRepository.TypeLink second=mock(EnvironmentTypeCompatibilityRepository.TypeLink.class);
        when(second.getParentTypeCode()).thenReturn("CUSTOM");
        when(second.getChildTypeCode()).thenReturn("SHARD");
        when(compatibilities.findActiveTypeLinksForValidation()).thenReturn(List.of(first,second));
        assertThrows(ValidationException.class,()->validator.validateForCreate("CUSTOM","SHARD",List.of(customType,shardType)));
    }

    @Test void requiresInactiveUnusedCompatibilityForDeletion(){
        var relation=new EnvironmentTypeCompatibility(customType,shardType,LocalDateTime.now());
        assertThrows(ValidationException.class,()->validator.validateForDelete(relation));
        relation.inactivate(LocalDateTime.now());
        when(environments.findUsageForCompatibilityValidation("CUSTOM","SHARD")).thenReturn(Optional.empty());
        validator.validateForDelete(relation);
        when(environments.findUsageForCompatibilityValidation("CUSTOM","SHARD")).thenReturn(Optional.of(3L));
        assertThrows(ValidationException.class,()->validator.validateForDelete(relation));
    }
}
