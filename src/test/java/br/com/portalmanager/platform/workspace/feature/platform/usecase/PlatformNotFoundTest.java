package br.com.portalmanager.platform.workspace.feature.platform.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.feature.platform.domain.PlatformMessageKeys;
import br.com.portalmanager.platform.workspace.feature.platform.domain.microservice.Microservice;
import br.com.portalmanager.platform.workspace.feature.platform.repository.featurecontext.FeatureContextRelationRepository;
import br.com.portalmanager.platform.workspace.feature.platform.repository.context.ContextRepository;
import br.com.portalmanager.platform.workspace.feature.platform.repository.feature.FeatureRepository;
import br.com.portalmanager.platform.workspace.feature.platform.repository.microservice.MicroserviceRepository;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.context.ContextQueryService;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.feature.FeatureFinder;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.feature.FeatureMapper;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.feature.FeatureQueryService;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.microservice.MicroserviceFinder;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.microservice.MicroserviceMapper;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.microservice.MicroserviceQueryService;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.validation.feature.FeatureValidator;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.validation.microservice.MicroserviceValidator;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.context.ContextFinder;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.validation.context.ContextValidator;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class PlatformNotFoundTest {

    @Test
    void missingResourcesUseTheirOwnNotFoundMessageKeys() {
        assertThatThrownBy(() ->
            microserviceQuery(mock(MicroserviceRepository.class)).findByIdentifier("missing")
        )
            .isInstanceOf(NotFoundException.class)
            .hasMessage(PlatformMessageKeys.MICROSERVICE_NOT_FOUND);
        assertThatThrownBy(() ->
            new FeatureQueryService(
                mock(FeatureRepository.class), mock(ContextRepository.class),
                new FeatureFinder(mock(FeatureRepository.class), mock(MicroserviceRepository.class), mock(EntityManager.class)),
                new FeatureMapper(JsonMapper.builder().build()),
                mock(FeatureValidator.class)
            ).findByIdentifier("missing")
        )
            .isInstanceOf(NotFoundException.class)
            .hasMessage(PlatformMessageKeys.FEATURE_NOT_FOUND);
        assertThatThrownBy(() ->
            new ContextQueryService(mock(ContextRepository.class), mock(FeatureRepository.class),
                new ContextFinder(mock(ContextRepository.class), mock(EntityManager.class)), mock(ContextValidator.class)).findByIdentifier("missing")
        )
            .isInstanceOf(NotFoundException.class)
            .hasMessage(PlatformMessageKeys.CONTEXT_NOT_FOUND);
    }

    @Test
    void inactiveServiceHasSpecificNotFoundMessageKeyForActiveLookup() {
        MicroserviceRepository repository = mock(MicroserviceRepository.class);
        Microservice inactive = new Microservice("microservice", "Microservice", null, LocalDateTime.now());
        inactive.inactivate(LocalDateTime.now());
        when(repository.findActiveInternalIdByIdentifier(inactive.getIdentifier())).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            microserviceQuery(repository).findActiveInternalIdByIdentifier(inactive.getIdentifier())
        )
            .isInstanceOf(NotFoundException.class)
            .hasMessage(PlatformMessageKeys.ACTIVE_MICROSERVICE_NOT_FOUND);
    }
    private static MicroserviceQueryService microserviceQuery(MicroserviceRepository repository) {
        return new MicroserviceQueryService(
            repository,
            new MicroserviceFinder(
                repository, mock(EntityManager.class)),
            new MicroserviceMapper(
                JsonMapper.builder().build()),
            mock(MicroserviceValidator.class)
        );
    }
}
