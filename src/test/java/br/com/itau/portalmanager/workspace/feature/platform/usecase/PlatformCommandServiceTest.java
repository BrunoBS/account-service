package br.com.itau.portalmanager.workspace.feature.platform.usecase;

import br.com.itau.portalmanager.workspace.feature.platform.domain.Feature;
import br.com.itau.portalmanager.workspace.feature.platform.domain.Service;
import br.com.itau.portalmanager.workspace.feature.platform.repository.FeatureRepository;
import br.com.itau.portalmanager.workspace.feature.platform.repository.ServiceRepository;
import br.com.itau.portalmanager.workspace.feature.platform.usecase.feature.FeatureCommandService;
import br.com.itau.portalmanager.workspace.feature.platform.usecase.service.ServiceCommandService;
import br.com.itau.portalmanager.workspace.feature.platform.usecase.model.CreateFeatureInput;
import br.com.itau.portalmanager.workspace.feature.platform.usecase.model.CreateServiceInput;
import br.com.itau.portalmanager.workspace.foundation.catalog.featurescopetype.domain.FeatureScopeType;
import br.com.itau.portalmanager.workspace.foundation.catalog.featurescopetype.repository.FeatureScopeTypeRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlatformCommandServiceTest {

    @Test
    void shouldRejectDuplicateServiceCode() {
        ServiceRepository repository = mock(ServiceRepository.class);
        when(repository.existsByCode("audit-service")).thenReturn(true);
        ServiceCommandService command = new ServiceCommandService(repository);

        assertThatThrownBy(() -> command.create(new CreateServiceInput("audit-service", "Audit Service", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Service code already exists");
    }

    @Test
    void shouldRejectDuplicateFeatureCode() {
        FeatureRepository features = mock(FeatureRepository.class);
        ServiceRepository services = mock(ServiceRepository.class);
        FeatureScopeTypeRepository scopes = mock(FeatureScopeTypeRepository.class);
        when(features.existsByCode("AUDIT")).thenReturn(true);
        FeatureCommandService command = new FeatureCommandService(features, services, scopes);

        assertThatThrownBy(() -> command.create(new CreateFeatureInput("AUDIT", "Audit", null, "service-id", "{}")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Feature code already exists");
    }

    @Test
    void shouldRejectFeatureCreationForInactiveService() {
        FeatureRepository features = mock(FeatureRepository.class);
        ServiceRepository services = mock(ServiceRepository.class);
        FeatureScopeTypeRepository scopes = mock(FeatureScopeTypeRepository.class);
        Service service = new Service("audit-service", "Audit Service", null, LocalDateTime.now());
        service.inactivate(LocalDateTime.now());
        when(features.existsByCode("AUDIT")).thenReturn(false);
        when(services.findByIdentifier(service.getIdentifier())).thenReturn(Optional.of(service));
        FeatureCommandService command = new FeatureCommandService(features, services, scopes);

        assertThatThrownBy(() -> command.create(new CreateFeatureInput("AUDIT", "Audit", null, service.getIdentifier(), "{}")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Feature requires an active service");
    }

    @Test
    void shouldRejectInactiveScopeAssociation() {
        FeatureRepository features = mock(FeatureRepository.class);
        ServiceRepository services = mock(ServiceRepository.class);
        FeatureScopeTypeRepository scopes = mock(FeatureScopeTypeRepository.class);
        Service service = new Service("audit-service", "Audit Service", null, LocalDateTime.now());
        Feature feature = new Feature("AUDIT", "Audit", null, service, "{}", LocalDateTime.now());
        FeatureScopeType scope = new FeatureScopeType();
        scope.setCode("ADMINISTRATION");
        scope.setActive(false);
        when(features.findByIdentifier(feature.getIdentifier())).thenReturn(Optional.of(feature));
        when(scopes.findById("ADMINISTRATION")).thenReturn(Optional.of(scope));
        FeatureCommandService command = new FeatureCommandService(features, services, scopes);

        assertThatThrownBy(() -> command.associateScope(feature.getIdentifier(), "ADMINISTRATION"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Feature scope must be active");
    }

    @Test
    void shouldPersistFeatureCreatedWithActiveService() {
        FeatureRepository features = mock(FeatureRepository.class);
        ServiceRepository services = mock(ServiceRepository.class);
        FeatureScopeTypeRepository scopes = mock(FeatureScopeTypeRepository.class);
        Service service = new Service("audit-service", "Audit Service", null, LocalDateTime.now());
        when(features.existsByCode("AUDIT")).thenReturn(false);
        when(services.findByIdentifier(service.getIdentifier())).thenReturn(Optional.of(service));
        when(features.save(any(Feature.class))).thenAnswer(invocation -> invocation.getArgument(0));
        FeatureCommandService command = new FeatureCommandService(features, services, scopes);

        command.create(new CreateFeatureInput("AUDIT", "Audit", null, service.getIdentifier(), "{}"));

        verify(features).save(any(Feature.class));
    }

    @Test
    void shouldRejectServiceQuarantineWhileItOwnsFeatures() {
        ServiceRepository repository = mock(ServiceRepository.class);
        Service service = new Service("audit-service", "Audit Service", null, LocalDateTime.now());
        new Feature("AUDIT", "Audit", null, service, "{}", LocalDateTime.now());
        when(repository.findByIdentifier(service.getIdentifier())).thenReturn(Optional.of(service));
        ServiceCommandService command = new ServiceCommandService(repository);

        assertThatThrownBy(() -> command.delete(service.getIdentifier()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Service with features cannot be quarantined");
    }
}
