package br.com.portalmanager.platform.workspace.unit.feature.servicecontext.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.ServiceContextMessageKeys;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.microservice.Microservice;
import br.com.portalmanager.platform.workspace.feature.servicecontext.repository.feature.FeatureRepository;
import br.com.portalmanager.platform.workspace.feature.servicecontext.repository.featuregroup.FeatureGroupRepository;
import br.com.portalmanager.platform.workspace.feature.servicecontext.repository.microservice.MicroserviceRepository;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.feature.FeatureFinder;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.mapping.feature.FeatureMapper;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.feature.FeatureQueryService;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.featuregroup.FeatureGroupFinder;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.featuregroup.FeatureGroupQueryService;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.microservice.MicroserviceFinder;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.mapping.microservice.MicroserviceMapper;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.microservice.MicroserviceQueryService;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.validation.feature.FeatureValidator;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.validation.featuregroup.FeatureGroupValidator;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.validation.microservice.MicroserviceValidator;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.Optional;

class ServiceContextNotFoundTest {

    @Test
    void missingResourcesUseTheirOwnNotFoundMessageKeys() {
        assertThatThrownBy(
                        () ->
                                microserviceQuery(mock(MicroserviceRepository.class))
                                        .findByIdentifier("missing"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(ServiceContextMessageKeys.MICROSERVICE_NOT_FOUND);
        assertThatThrownBy(
                        () ->
                                new FeatureQueryService(
                                                mock(FeatureRepository.class),
                                                mock(FeatureGroupRepository.class),
                                                new FeatureFinder(
                                                        mock(FeatureRepository.class),
                                                        mock(MicroserviceRepository.class),
                                                        mock(EntityManager.class)),
                                                new FeatureMapper(JsonMapper.builder().build()),
                                                mock(FeatureValidator.class))
                                        .findByIdentifier("missing"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(ServiceContextMessageKeys.FEATURE_NOT_FOUND);
        assertThatThrownBy(
                        () ->
                                new FeatureGroupQueryService(
                                                mock(FeatureGroupRepository.class),
                                                mock(FeatureRepository.class),
                                                new FeatureGroupFinder(
                                                        mock(FeatureGroupRepository.class),
                                                        mock(EntityManager.class)),
                                                mock(FeatureGroupValidator.class),
                                                new br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.mapping.featuregroup.FeatureGroupMapper())
                                        .findByIdentifier("missing"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(ServiceContextMessageKeys.FEATURE_GROUP_NOT_FOUND);
    }

    @Test
    void inactiveServiceHasSpecificNotFoundMessageKeyForActiveLookup() {
        MicroserviceRepository repository = mock(MicroserviceRepository.class);
        Microservice inactive =
                new Microservice("microservice", "Microservice", null, LocalDateTime.now());
        inactive.inactivate(LocalDateTime.now());
        when(repository.findActiveInternalIdByIdentifier(inactive.getIdentifier()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                microserviceQuery(repository)
                                        .findActiveInternalIdByIdentifier(inactive.getIdentifier()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(ServiceContextMessageKeys.ACTIVE_MICROSERVICE_NOT_FOUND);
    }

    private static MicroserviceQueryService microserviceQuery(MicroserviceRepository repository) {
        return new MicroserviceQueryService(
                repository,
                new MicroserviceFinder(repository, mock(EntityManager.class)),
                new MicroserviceMapper(JsonMapper.builder().build()),
                mock(MicroserviceValidator.class));
    }
}
