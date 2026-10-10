package br.com.portalmanager.platform.workspace.feature.platform.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.feature.platform.domain.feature.Feature;
import br.com.portalmanager.platform.workspace.feature.platform.domain.featurecontext.FeatureContext;
import br.com.portalmanager.platform.workspace.feature.platform.domain.microservice.Microservice;
import br.com.portalmanager.platform.workspace.feature.platform.repository.FeatureContextRelationRepository;
import br.com.portalmanager.platform.workspace.feature.platform.repository.FeatureContextRepository;
import br.com.portalmanager.platform.workspace.feature.platform.repository.FeatureRepository;
import br.com.portalmanager.platform.workspace.feature.platform.repository.MicroserviceRepository;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.CreateFeatureContextInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.CreateFeatureInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.CreateMicroserviceInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.context.FeatureContextCommandService;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.feature.FeatureCommandService;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.microservice.MicroserviceCommandService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PlatformCommandServiceTest {

    @Test
    void shouldRejectInvalidCodeThroughUseCase() {
        MicroserviceRepository services = mock(MicroserviceRepository.class);
        MicroserviceCommandService command = new MicroserviceCommandService(services);

        assertThatThrownBy(() ->
            command.create(new CreateMicroserviceInput("AUDIT_SERVICE", "Audit Service", null))
        ).isInstanceOf(ValidationException.class);
        verify(services, never()).save(any(Microservice.class));
    }

    @Test
    void shouldRejectInvalidContextCodeThroughUseCase() {
        FeatureContextRepository contexts = mock(FeatureContextRepository.class);
        FeatureContextCommandService command = new FeatureContextCommandService(
            contexts,
            mock(FeatureContextRelationRepository.class)
        );

        assertThatThrownBy(() ->
            command.create(new CreateFeatureContextInput("MANAGER_ACCOUNT", "Manager Account", null))
        ).isInstanceOf(ValidationException.class);
        verify(contexts, never()).save(any(FeatureContext.class));
    }

    @Test
    void shouldRejectInactiveServiceWhenCreatingFeature() {
        FeatureRepository features = mock(FeatureRepository.class);
        MicroserviceRepository services = mock(MicroserviceRepository.class);
        FeatureContextRepository contexts = mock(FeatureContextRepository.class);
        Microservice microservice = new Microservice("audit-service", "Audit Service", null, LocalDateTime.now());
        microservice.inactivate(LocalDateTime.now());
        when(services.findByIdentifier(microservice.getIdentifier())).thenReturn(Optional.of(microservice));
        FeatureCommandService command = new FeatureCommandService(
            features,
            services,
            contexts,
            mock(FeatureContextRelationRepository.class)
        );

        assertThatThrownBy(() ->
            command.create(new CreateFeatureInput("audit", "Audit", null, microservice.getIdentifier(), "{}", false))
        ).isInstanceOf(ValidationException.class);
        verify(features, never()).save(any(Feature.class));
    }

    @Test
    void shouldRejectFeatureActivationWhenServiceIsInactive() {
        FeatureRepository features = mock(FeatureRepository.class);
        MicroserviceRepository services = mock(MicroserviceRepository.class);
        FeatureContextRepository contexts = mock(FeatureContextRepository.class);
        Microservice microservice = new Microservice("audit-service", "Audit Service", null, LocalDateTime.now());
        Feature feature = new Feature("audit", "Audit", null, microservice, "{}", LocalDateTime.now());
        feature.inactivate(LocalDateTime.now());
        microservice.inactivate(LocalDateTime.now());
        when(features.findByIdentifier(feature.getIdentifier())).thenReturn(Optional.of(feature));
        FeatureCommandService command = new FeatureCommandService(
            features,
            services,
            contexts,
            mock(FeatureContextRelationRepository.class)
        );

        assertThatThrownBy(() -> command.activate(feature.getIdentifier())).isInstanceOf(ValidationException.class);
        verify(features, never()).save(any(Feature.class));
    }

    @Test
    void shouldRejectDuplicateMicroserviceCode() {
        MicroserviceRepository repository = mock(MicroserviceRepository.class);
        when(repository.existsByCode("audit-service")).thenReturn(true);
        MicroserviceCommandService command = new MicroserviceCommandService(repository);

        assertThatThrownBy(() ->
            command.create(new CreateMicroserviceInput("audit-service", "Audit Service", null))
        ).isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldRejectDuplicateServiceName() {
        MicroserviceRepository repository = mock(MicroserviceRepository.class);
        when(repository.existsByName("Audit Service")).thenReturn(true);
        MicroserviceCommandService command = new MicroserviceCommandService(repository);

        assertThatThrownBy(() ->
            command.create(new CreateMicroserviceInput("audit-service", "Audit Service", null))
        ).isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldRejectDuplicateFeatureName() {
        FeatureRepository features = mock(FeatureRepository.class);
        MicroserviceRepository services = mock(MicroserviceRepository.class);
        FeatureContextRepository contexts = mock(FeatureContextRepository.class);
        when(features.existsByName("Audit")).thenReturn(true);
        FeatureCommandService command = new FeatureCommandService(
            features,
            services,
            contexts,
            mock(FeatureContextRelationRepository.class)
        );

        assertThatThrownBy(() ->
            command.create(new CreateFeatureInput("audit", "Audit", null, "microservice-id", "{}", false))
        ).isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldRejectDuplicateFeatureContextName() {
        FeatureContextRepository contexts = mock(FeatureContextRepository.class);
        when(contexts.existsByName("Manager Account")).thenReturn(true);
        FeatureContextCommandService command = new FeatureContextCommandService(
            contexts,
            mock(FeatureContextRelationRepository.class)
        );

        assertThatThrownBy(() ->
            command.create(new CreateFeatureContextInput("manager-account", "Manager Account", null))
        ).isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldRejectFeatureContextQuarantineWhileItOwnsFeatures() {
        FeatureContextRepository contexts = mock(FeatureContextRepository.class);
        FeatureContext context = new FeatureContext("manager-account", "Manager Account", null, LocalDateTime.now());
        FeatureContextRelationRepository relations = mock(FeatureContextRelationRepository.class);
        when(relations.existsByContextId(context.getId())).thenReturn(true);

        when(contexts.findByIdentifierForUpdate(context.getIdentifier())).thenReturn(Optional.of(context));
        FeatureContextCommandService command = new FeatureContextCommandService(contexts, relations);

        assertThatThrownBy(() -> command.delete(context.getIdentifier())).isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldRejectInactiveContextAssociation() {
        FeatureRepository features = mock(FeatureRepository.class);
        MicroserviceRepository services = mock(MicroserviceRepository.class);
        FeatureContextRepository contexts = mock(FeatureContextRepository.class);
        Microservice microservice = new Microservice("audit-service", "Audit Service", null, LocalDateTime.now());
        Feature feature = new Feature("audit", "Audit", null, microservice, "{}", LocalDateTime.now());
        FeatureContext context = new FeatureContext("administration", "administration", null, LocalDateTime.now());
        context.inactivate(LocalDateTime.now());

        when(features.findByIdentifierForUpdate(feature.getIdentifier())).thenReturn(Optional.of(feature));
        when(contexts.findByIdentifierForUpdate(context.getIdentifier())).thenReturn(Optional.of(context));
        FeatureCommandService command = new FeatureCommandService(
            features,
            services,
            contexts,
            mock(FeatureContextRelationRepository.class)
        );

        assertThatThrownBy(() ->
            command.associateContext(feature.getIdentifier(), context.getIdentifier())
        ).isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldPersistFeatureCreatedWithActiveService() {
        FeatureRepository features = mock(FeatureRepository.class);
        MicroserviceRepository services = mock(MicroserviceRepository.class);
        FeatureContextRepository contexts = mock(FeatureContextRepository.class);
        Microservice microservice = new Microservice("audit-service", "Audit Service", null, LocalDateTime.now());
        when(services.findByIdentifier(microservice.getIdentifier())).thenReturn(Optional.of(microservice));
        when(features.save(any(Feature.class))).thenAnswer(invocation -> invocation.getArgument(0));
        FeatureCommandService command = new FeatureCommandService(
            features,
            services,
            contexts,
            mock(FeatureContextRelationRepository.class)
        );

        command.create(new CreateFeatureInput("audit", "Audit", null, microservice.getIdentifier(), "{}", false));

        verify(features).save(any(Feature.class));
    }

    @Test
    void shouldRejectServiceQuarantineWhileItOwnsFeatures() {
        MicroserviceRepository repository = mock(MicroserviceRepository.class);
        Microservice microservice = new Microservice("audit-service", "Audit Service", null, LocalDateTime.now());
        new Feature("audit", "Audit", null, microservice, "{}", LocalDateTime.now());
        when(repository.findByIdentifier(microservice.getIdentifier())).thenReturn(Optional.of(microservice));
        MicroserviceCommandService command = new MicroserviceCommandService(repository);

        assertThatThrownBy(() -> command.delete(microservice.getIdentifier())).isInstanceOf(ValidationException.class);
    }
}
