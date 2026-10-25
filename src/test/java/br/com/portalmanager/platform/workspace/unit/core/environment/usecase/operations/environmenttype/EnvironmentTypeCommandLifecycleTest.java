package br.com.portalmanager.platform.workspace.unit.core.environment.usecase.operations.environmenttype;


import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environmenttype.EnvironmentTypeCommandService;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environmenttype.EnvironmentTypeFinder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentType;
import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentTypeDetails;
import br.com.portalmanager.platform.workspace.core.environment.repository.compatibility.EnvironmentTypeCompatibilityRepository;
import br.com.portalmanager.platform.workspace.core.environment.repository.environment.EnvironmentRepository;
import br.com.portalmanager.platform.workspace.core.environment.repository.environmenttype.EnvironmentTypeRepository;
import br.com.portalmanager.platform.workspace.core.environment.usecase.mapping.environmenttype.EnvironmentTypeMapper;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environmenttype.EnvironmentTypeInput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environmenttype.EnvironmentTypeOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.validation.environmenttype.EnvironmentTypeValidator;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class EnvironmentTypeCommandLifecycleTest {
    private final EnvironmentTypeRepository types=mock(EnvironmentTypeRepository.class);
    private final EnvironmentTypeFinder finder=mock(EnvironmentTypeFinder.class);
    private final EnvironmentRepository environments=mock(EnvironmentRepository.class);
    private final EnvironmentTypeCompatibilityRepository compatibilities=mock(EnvironmentTypeCompatibilityRepository.class);
    private final EnvironmentTypeMapper mapper=mock(EnvironmentTypeMapper.class);
    private final EnvironmentTypeValidator validator=new EnvironmentTypeValidator(types,environments,compatibilities);
    private final EnvironmentTypeCommandService command=new EnvironmentTypeCommandService(types,finder,validator,mapper);
    private EnvironmentType type(){var entity=new EnvironmentType("CELL",new EnvironmentTypeDetails("Cell","Terminal type",false,false,true,4),LocalDateTime.now());ReflectionTestUtils.setField(entity,"version",0L);return entity;}
    private EnvironmentTypeInput input(long version){return new EnvironmentTypeInput(version,"CELL","Updated Cell","Updated terminal type",false,false,true,3);}

    @Test void createsUpdatesChangesLifecycleAndDeletesUnusedType(){
        var entity=type(); var details=new EnvironmentTypeDetails("Cell","Terminal type",false,false,true,4);
        var output=new EnvironmentTypeOutput(0L,"CELL","CELL","Cell","Terminal type",false,false,true,4,"ACTIVE");
        when(types.existsByCode("CELL")).thenReturn(false); when(mapper.toDetails(any())).thenReturn(details);
        when(types.saveAndFlush(any(EnvironmentType.class))).thenAnswer(invocation->invocation.getArgument(0));
        when(mapper.toOutput(any(EnvironmentType.class))).thenReturn(output);
        assertEquals(output,command.create(new EnvironmentTypeInput(null,"CELL","Cell","Terminal type",false,false,true,4)));

        when(finder.findForConfigurationUpdate("CELL")).thenReturn(entity);
        when(mapper.toDetails(input(0L))).thenReturn(new EnvironmentTypeDetails("Updated Cell","Updated terminal type",false,false,true,3));
        assertEquals(output,command.update("CELL",input(0L)));
        when(finder.findForUpdate("CELL")).thenReturn(entity);
        command.inactivate("CELL"); assertEquals("INACTIVE",entity.getLifecycle().value());
        command.restore("CELL"); assertEquals("ACTIVE",entity.getLifecycle().value());
        command.inactivate("CELL");
        when(environments.findUsageForTypeValidation("CELL")).thenReturn(Optional.empty());
        when(compatibilities.findUsageForTypeValidation("CELL")).thenReturn(Optional.empty());
        assertEquals(output,command.delete("CELL"));
        verify(types).delete(entity); verify(types).flush();
    }
}
