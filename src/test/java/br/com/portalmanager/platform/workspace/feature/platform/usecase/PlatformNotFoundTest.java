package br.com.portalmanager.platform.workspace.feature.platform.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.feature.platform.domain.Microservice;
import br.com.portalmanager.platform.workspace.feature.platform.repository.FeatureContextRepository;
import br.com.portalmanager.platform.workspace.feature.platform.repository.FeatureRepository;
import br.com.portalmanager.platform.workspace.feature.platform.repository.MicroserviceRepository;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.PlatformMessageKeys;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.context.FeatureContextQueryService;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.feature.FeatureQueryService;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.microservice.MicroserviceQueryService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PlatformNotFoundTest {

    @Test
    void missingResourcesUseTheirOwnNotFoundMessageKeys() {
        assertThatThrownBy(() ->
            new MicroserviceQueryService(mock(MicroserviceRepository.class)).findByIdentifier("missing")
        )
            .isInstanceOf(NotFoundException.class)
            .hasMessage(PlatformMessageKeys.MICROSERVICE_NOT_FOUND);
        assertThatThrownBy(() -> new FeatureQueryService(mock(FeatureRepository.class)).findByIdentifier("missing"))
            .isInstanceOf(NotFoundException.class)
            .hasMessage(PlatformMessageKeys.FEATURE_NOT_FOUND);
        assertThatThrownBy(() ->
            new FeatureContextQueryService(mock(FeatureContextRepository.class)).findByIdentifier("missing")
        )
            .isInstanceOf(NotFoundException.class)
            .hasMessage(PlatformMessageKeys.CONTEXT_NOT_FOUND);
    }

    @Test
    void inactiveServiceHasSpecificNotFoundMessageKeyForActiveLookup() {
        MicroserviceRepository repository = mock(MicroserviceRepository.class);
        Microservice inactive = new Microservice("microservice", "Microservice", null, LocalDateTime.now());
        inactive.inactivate(LocalDateTime.now());
        when(repository.findByIdentifier(inactive.getIdentifier())).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() ->
            new MicroserviceQueryService(repository).findActiveInternalIdByIdentifier(inactive.getIdentifier())
        )
            .isInstanceOf(NotFoundException.class)
            .hasMessage(PlatformMessageKeys.ACTIVE_MICROSERVICE_NOT_FOUND);
    }
}
