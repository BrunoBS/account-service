package br.com.portalmanager.platform.workspace.feature.platform.usecase;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.feature.platform.domain.Service;
import br.com.portalmanager.platform.workspace.feature.platform.repository.FeatureContextRepository;
import br.com.portalmanager.platform.workspace.feature.platform.repository.FeatureRepository;
import br.com.portalmanager.platform.workspace.feature.platform.repository.ServiceRepository;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.PlatformMessageKeys;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.context.FeatureContextQueryService;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.feature.FeatureQueryService;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.service.ServiceQueryService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlatformNotFoundTest {

    @Test
    void missingResourcesUseTheirOwnNotFoundMessageKeys() {
        assertThatThrownBy(() -> new ServiceQueryService(mock(ServiceRepository.class)).findByIdentifier("missing"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(PlatformMessageKeys.SERVICE_NOT_FOUND);
        assertThatThrownBy(() -> new FeatureQueryService(mock(FeatureRepository.class)).findByIdentifier("missing"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(PlatformMessageKeys.FEATURE_NOT_FOUND);
        assertThatThrownBy(() -> new FeatureContextQueryService(mock(FeatureContextRepository.class))
                .findByIdentifier("missing"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(PlatformMessageKeys.CONTEXT_NOT_FOUND);
    }

    @Test
    void inactiveServiceHasSpecificNotFoundMessageKeyForActiveLookup() {
        ServiceRepository repository = mock(ServiceRepository.class);
        Service inactive = new Service("service", "Service", null, LocalDateTime.now());
        inactive.inactivate(LocalDateTime.now());
        when(repository.findByIdentifier(inactive.getIdentifier())).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> new ServiceQueryService(repository)
                .findActiveInternalIdByIdentifier(inactive.getIdentifier()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(PlatformMessageKeys.ACTIVE_SERVICE_NOT_FOUND);
    }
}
