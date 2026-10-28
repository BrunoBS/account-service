package br.com.portalmanager.platform.workspace.feature.parameter.usecase.operations;

import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.workspace.feature.parameter.repository.ParameterKeyRepository;
import br.com.portalmanager.platform.workspace.feature.parameter.repository.projection.ParameterKeyReadProjection;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.port.MicroserviceReferencePort;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.validation.ParameterKeyValidator;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ParameterKeyPublicationCommandServiceTest {
    @Test void affectedRowMismatchIsAConflictRatherThanSuccessfulPublication() {
        ParameterKeyRepository repository = mock(ParameterKeyRepository.class);
        ParameterKeyValidator validator = mock(ParameterKeyValidator.class);
        MicroserviceReferencePort microservices = mock(MicroserviceReferencePort.class);
        ParameterKeyReadProjection first = draft(1L);
        ParameterKeyReadProjection second = draft(2L);
        when(repository.findDraftsForPublication(List.of(1L, 2L), List.of(7L), "DRAFT"))
                .thenReturn(List.of(first, second));
        when(microservices.findByInternalIds(List.of(7L))).thenReturn(Map.of(7L,
                new MicroserviceReferencePort.Reference(7L, "identifier", "service")));
        when(repository.publishDrafts(anyCollection(), anyCollection(), eq("DRAFT"), eq("PUBLISHED"), any())).thenReturn(1);
        ParameterKeyPublicationCommandService commands = new ParameterKeyPublicationCommandService(repository, validator, microservices);
        assertThatThrownBy(() -> commands.publishBatch(List.of(1L, 2L), List.of(7L)))
                .isInstanceOf(ResourceVersionConflictException.class);
    }

    @Test void noLongerEligibleRowsDoNotTriggerAnUpdateOrAuditSnapshot() {
        ParameterKeyRepository repository = mock(ParameterKeyRepository.class);
        ParameterKeyValidator validator = mock(ParameterKeyValidator.class);
        MicroserviceReferencePort microservices = mock(MicroserviceReferencePort.class);
        ParameterKeyPublicationCommandService commands = new ParameterKeyPublicationCommandService(repository, validator, microservices);
        assertThat(commands.publishBatch(List.of(1L), List.of(7L))).isEmpty();
        verify(repository, never()).publishDrafts(anyCollection(), anyCollection(), anyString(), anyString(), any());
        verifyNoInteractions(validator, microservices);
    }

    private ParameterKeyReadProjection draft(Long id) {
        ParameterKeyReadProjection result = mock(ParameterKeyReadProjection.class);
        when(result.getId()).thenReturn(id);
        when(result.getMicroserviceId()).thenReturn(7L);
        return result;
    }
}
