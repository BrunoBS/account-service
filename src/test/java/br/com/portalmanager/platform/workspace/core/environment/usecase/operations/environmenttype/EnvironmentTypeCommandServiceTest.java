package br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environmenttype;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.core.environment.repository.environment.EnvironmentRepository;
import br.com.portalmanager.platform.workspace.core.environment.repository.compatibility.EnvironmentTypeCompatibilityRepository;
import br.com.portalmanager.platform.workspace.core.environment.repository.environmenttype.EnvironmentTypeRepository;
import br.com.portalmanager.platform.workspace.core.environment.usecase.mapping.environmenttype.EnvironmentTypeMapper;
import br.com.portalmanager.platform.workspace.core.environment.usecase.validation.environmenttype.EnvironmentTypeValidator;
import org.junit.jupiter.api.Test;

class EnvironmentTypeCommandServiceTest {

    @Test
    void rejectsNullInputBeforeConsultingOrWritingTheDatabase() {
        EnvironmentTypeRepository types = mock(EnvironmentTypeRepository.class);
        EnvironmentTypeFinder finder = mock(EnvironmentTypeFinder.class);
        EnvironmentTypeMapper mapper = mock(EnvironmentTypeMapper.class);
        EnvironmentTypeCommandService command = new EnvironmentTypeCommandService(
            types,
            finder,
            new EnvironmentTypeValidator(types, mock(EnvironmentRepository.class), mock(EnvironmentTypeCompatibilityRepository.class)),
            mapper
        );
        assertThatThrownBy(() -> command.create(null)).isInstanceOf(ValidationException.class);
        verifyNoInteractions(types, finder, mapper);
    }
}
