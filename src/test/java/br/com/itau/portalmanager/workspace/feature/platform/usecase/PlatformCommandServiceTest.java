package br.com.itau.portalmanager.workspace.feature.platform.usecase;

import br.com.itau.portalmanager.workspace.feature.platform.domain.Feature;
import br.com.itau.portalmanager.workspace.feature.platform.domain.FeatureContext;
import br.com.itau.portalmanager.workspace.feature.platform.domain.Service;
import br.com.itau.portalmanager.workspace.feature.platform.repository.FeatureContextRepository;
import br.com.itau.portalmanager.workspace.feature.platform.repository.FeatureRepository;
import br.com.itau.portalmanager.workspace.feature.platform.repository.ServiceRepository;
import br.com.itau.portalmanager.workspace.feature.platform.usecase.context.FeatureContextCommandService;
import br.com.itau.portalmanager.workspace.feature.platform.usecase.feature.FeatureCommandService;
import br.com.itau.portalmanager.workspace.feature.platform.usecase.model.CreateFeatureContextInput;
import br.com.itau.portalmanager.workspace.feature.platform.usecase.model.CreateFeatureInput;
import br.com.itau.portalmanager.workspace.feature.platform.usecase.model.CreateServiceInput;
import br.com.itau.portalmanager.workspace.feature.platform.usecase.service.ServiceCommandService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PlatformCommandServiceTest {

    @Test
    void shouldRejectDuplicateServiceCode() {
        ServiceRepository repository = mock(ServiceRepository.class);
        when(repository.existsByCode("AUDIT_SERVICE")).thenReturn(true);
        ServiceCommandService command = new ServiceCommandService(repository);

        assertThatThrownBy(() -> command.create(new CreateServiceInput("AUDIT_SERVICE", "Audit Service", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Service code already exists");
    }

    @Test
    void shouldRejectDuplicateServiceName() {
        ServiceRepository repository = mock(ServiceRepository.class);
        when(repository.existsByName("Audit Service")).thenReturn(true);
        ServiceCommandService command = new ServiceCommandService(repository);

        assertThatThrownBy(() -> command.create(new CreateServiceInput("AUDIT_SERVICE", "Audit Service", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Service name already exists");
    }

    @Test
    void shouldRejectDuplicateFeatureName() {
        FeatureRepository features = mock(FeatureRepository.class);
        ServiceRepository services = mock(ServiceRepository.class);
        FeatureContextRepository contexts = mock(FeatureContextRepository.class);
        when(features.existsByName("Audit")).thenReturn(true);
        FeatureCommandService command = new FeatureCommandService(features, services, contexts);

        assertThatThrownBy(() -> command.create(new CreateFeatureInput("AUDIT", "Audit", null, "service-id", "{}")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Feature name already exists");
    }

    @Test
    void shouldRejectDuplicateFeatureContextName() {
        FeatureContextRepository contexts = mock(FeatureContextRepository.class);
        when(contexts.existsByName("Manager Account")).thenReturn(true);
        FeatureContextCommandService command = new FeatureContextCommandService(contexts);

        assertThatThrownBy(() -> command.create(
                new CreateFeatureContextInput("MANAGER_ACCOUNT", "Manager Account", null)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Feature context name already exists");
    }

    @Test
    void shouldRejectFeatureContextQuarantineWhileItOwnsFeatures() {
        FeatureContextRepository contexts = mock(FeatureContextRepository.class);
        FeatureContext context = new FeatureContext("MANAGER_ACCOUNT", "Manager Account", null, LocalDateTime.now());
        Service service = new Service("PORTAL_MANAGER", "Portal Manager", null, LocalDateTime.now());
        Feature feature = new Feature("APPLICATION", "Application", null, service, "{}", LocalDateTime.now());
        feature.addContext(context);

        when(contexts.findByIdentifier(context.getIdentifier())).thenReturn(Optional.of(context));
        FeatureContextCommandService command = new FeatureContextCommandService(contexts);

        assertThatThrownBy(() -> command.delete(context.getIdentifier()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Feature context with features cannot be quarantined");
    }

    @Test
    void shouldRejectInactiveContextAssociation() {
        FeatureRepository features = mock(FeatureRepository.class);
        ServiceRepository services = mock(ServiceRepository.class);
        FeatureContextRepository contexts = mock(FeatureContextRepository.class);
        Service service = new Service("AUDIT_SERVICE", "Audit Service", null, LocalDateTime.now());
        Feature feature = new Feature("AUDIT", "Audit", null, service, "{}", LocalDateTime.now());
        FeatureContext context = new FeatureContext("ADMINISTRATION", "ADMINISTRATION", null, LocalDateTime.now());
        context.inactivate(LocalDateTime.now());

        when(features.findByIdentifier(feature.getIdentifier())).thenReturn(Optional.of(feature));
        when(contexts.findByIdentifier(context.getIdentifier())).thenReturn(Optional.of(context));
        FeatureCommandService command = new FeatureCommandService(features, services, contexts);

        assertThatThrownBy(() -> command.associateContext(feature.getIdentifier(), context.getIdentifier()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Feature context must be active");
    }

    @Test
    void shouldPersistFeatureCreatedWithActiveService() {
        FeatureRepository features = mock(FeatureRepository.class);
        ServiceRepository services = mock(ServiceRepository.class);
        FeatureContextRepository contexts = mock(FeatureContextRepository.class);
        Service service = new Service("AUDIT_SERVICE", "Audit Service", null, LocalDateTime.now());
        when(services.findByIdentifier(service.getIdentifier())).thenReturn(Optional.of(service));
        when(features.save(any(Feature.class))).thenAnswer(invocation -> invocation.getArgument(0));
        FeatureCommandService command = new FeatureCommandService(features, services, contexts);

        command.create(new CreateFeatureInput("AUDIT", "Audit", null, service.getIdentifier(), "{}"));

        verify(features).save(any(Feature.class));
    }

    @Test
    void shouldRejectServiceQuarantineWhileItOwnsFeatures() {
        ServiceRepository repository = mock(ServiceRepository.class);
        Service service = new Service("AUDIT_SERVICE", "Audit Service", null, LocalDateTime.now());
        new Feature("AUDIT", "Audit", null, service, "{}", LocalDateTime.now());
        when(repository.findByIdentifier(service.getIdentifier())).thenReturn(Optional.of(service));
        ServiceCommandService command = new ServiceCommandService(repository);

        assertThatThrownBy(() -> command.delete(service.getIdentifier()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Service with features cannot be quarantined");
    }
}
