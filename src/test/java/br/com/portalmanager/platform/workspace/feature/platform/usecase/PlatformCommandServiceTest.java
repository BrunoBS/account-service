package br.com.portalmanager.platform.workspace.feature.platform.usecase;

import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.featurecontext.FeatureContextCommandService;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.validation.featurecontext.FeatureContextValidator;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.feature.platform.domain.feature.Feature;
import br.com.portalmanager.platform.workspace.feature.platform.domain.context.Context;
import br.com.portalmanager.platform.workspace.feature.platform.domain.microservice.Microservice;
import br.com.portalmanager.platform.workspace.feature.platform.repository.featurecontext.FeatureContextRelationRepository;
import br.com.portalmanager.platform.workspace.feature.platform.repository.context.ContextRepository;
import br.com.portalmanager.platform.workspace.feature.platform.repository.feature.FeatureRepository;
import br.com.portalmanager.platform.workspace.feature.platform.repository.microservice.MicroserviceRepository;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.context.CreateContextInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.feature.CreateFeatureInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.microservice.CreateMicroserviceInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.context.ContextCommandService;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.feature.FeatureCommandService;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.featurecontext.FeatureContextAssociationOperations;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.feature.FeatureFinder;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.feature.FeatureMapper;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.microservice.MicroserviceCommandService;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.microservice.MicroserviceFinder;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.microservice.MicroserviceMapper;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.validation.context.ContextValidator;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.validation.feature.FeatureValidator;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.validation.microservice.MicroserviceValidator;
import br.com.portalmanager.platform.workspace.foundation.integration.FeatureSharingReferencePort;
import br.com.portalmanager.platform.workspace.foundation.schema.integration.SchemaSettingsValidationAdapter;
import jakarta.persistence.EntityManager;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.context.ContextFinder;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class PlatformCommandServiceTest {

    @Test
    void shouldRejectVersionOnCreateThroughUseCase() {
        MicroserviceRepository services = mock(MicroserviceRepository.class);
        MicroserviceCommandService command = microserviceCommand(services);

        assertThatThrownBy(() ->
            command.create(new CreateMicroserviceInput(1L, "audit-service", "Audit Service", null, "{}"))
        ).isInstanceOf(ValidationException.class);
        verifyNoInteractions(services);
    }

    @Test
    void shouldRejectContextVersionOnCreateBeforeQuerying() {
        ContextRepository contexts = mock(ContextRepository.class);
        ContextCommandService command = contextCommand(
            contexts,
            mock(FeatureContextRelationRepository.class)
        );

        assertThatThrownBy(() ->
            command.create(new CreateContextInput(1L, "manager-account", "Manager Account", null))
        ).isInstanceOf(ValidationException.class);
        verify(contexts, never()).save(any(Context.class));
    }

    @Test
    void shouldRejectInactiveServiceWhenCreatingFeature() {
        FeatureRepository features = mock(FeatureRepository.class);
        MicroserviceRepository services = mock(MicroserviceRepository.class);
        ContextRepository contexts = mock(ContextRepository.class);
        Microservice microservice = new Microservice("audit-service", "Audit Service", null, LocalDateTime.now());
        microservice.inactivate(LocalDateTime.now());
        when(services.existsByIdentifier(microservice.getIdentifier())).thenReturn(true);
        FeatureCommandService command = featureCommand(features, services);

        assertThatThrownBy(() ->
            command.create(new CreateFeatureInput(null, "audit", "Audit", null, microservice.getIdentifier(), "{}", false))
        ).isInstanceOf(ValidationException.class);
        verify(features, never()).save(any(Feature.class));
    }

    @Test
    void shouldRejectFeatureActivationWhenServiceIsInactive() {
        FeatureRepository features = mock(FeatureRepository.class);
        MicroserviceRepository services = mock(MicroserviceRepository.class);
        ContextRepository contexts = mock(ContextRepository.class);
        Microservice microservice = new Microservice("audit-service", "Audit Service", null, LocalDateTime.now());
        Feature feature = new Feature("audit", "Audit", null, microservice, "{}", LocalDateTime.now());
        feature.inactivate(LocalDateTime.now());
        microservice.inactivate(LocalDateTime.now());
        when(features.findMicroserviceIdentifierByIdentifier(feature.getIdentifier())).thenReturn(Optional.of(microservice.getIdentifier()));
        when(services.existsByIdentifier(microservice.getIdentifier())).thenReturn(true);
        when(services.existsByIdentifier(microservice.getIdentifier())).thenReturn(true);
        FeatureCommandService command = featureCommand(features, services);

        assertThatThrownBy(() -> command.activate(feature.getIdentifier())).isInstanceOf(ValidationException.class);
        verify(features, never()).save(any(Feature.class));
    }

    @Test
    void shouldRejectDuplicateMicroserviceCode() {
        MicroserviceRepository repository = mock(MicroserviceRepository.class);
        when(repository.existsByCode("audit-service")).thenReturn(true);
        MicroserviceCommandService command = microserviceCommand(repository);

        assertThatThrownBy(() ->
            command.create(new CreateMicroserviceInput(null, "audit-service", "Audit Service", null, "{}"))
        ).isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldRejectDuplicateServiceName() {
        MicroserviceRepository repository = mock(MicroserviceRepository.class);
        when(repository.existsByName("Audit Service")).thenReturn(true);
        MicroserviceCommandService command = microserviceCommand(repository);

        assertThatThrownBy(() ->
            command.create(new CreateMicroserviceInput(null, "audit-service", "Audit Service", null, "{}"))
        ).isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldRejectDuplicateFeatureName() {
        FeatureRepository features = mock(FeatureRepository.class);
        MicroserviceRepository services = mock(MicroserviceRepository.class);
        ContextRepository contexts = mock(ContextRepository.class);
        when(features.existsByName("Audit")).thenReturn(true);
        FeatureCommandService command = featureCommand(features, services);

        assertThatThrownBy(() ->
            command.create(new CreateFeatureInput(null, "audit", "Audit", null, "microservice-id", "{}", false))
        ).isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldRejectDuplicateFeatureContextName() {
        ContextRepository contexts = mock(ContextRepository.class);
        when(contexts.existsByName("Manager Account")).thenReturn(true);
        ContextCommandService command = contextCommand(
            contexts,
            mock(FeatureContextRelationRepository.class)
        );

        assertThatThrownBy(() ->
            command.create(new CreateContextInput("manager-account", "Manager Account", null))
        ).isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldRejectContextPurgeWhileItOwnsFeatures() {
        ContextRepository contexts = mock(ContextRepository.class);
        Context context = new Context("manager-account", "Manager Account", null, LocalDateTime.now());
        FeatureContextRelationRepository relations = mock(FeatureContextRelationRepository.class);
        context.inactivate(LocalDateTime.now());
        when(relations.findFirstByContextId(context.getId())).thenReturn(
            Optional.of(mock(FeatureContextRelationRepository.RelationIdReference.class)));

        when(contexts.findByIdentifierForUpdate(context.getIdentifier())).thenReturn(Optional.of(context));
        when(contexts.findInternalIdByIdentifierForUpdate(context.getIdentifier())).thenReturn(Optional.of(1L));
        ContextCommandService command = contextCommand(contexts, relations);

        assertThatThrownBy(() -> command.purge(context.getIdentifier())).isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldRejectInactiveContextAssociation() {
        FeatureRepository features = mock(FeatureRepository.class);
        MicroserviceRepository services = mock(MicroserviceRepository.class);
        ContextRepository contexts = mock(ContextRepository.class);
        Microservice microservice = new Microservice("audit-service", "Audit Service", null, LocalDateTime.now());
        Feature feature = new Feature("audit", "Audit", null, microservice, "{}", LocalDateTime.now());
        Context context = new Context("administration", "administration", null, LocalDateTime.now());
        context.inactivate(LocalDateTime.now());

        when(features.findByIdentifierForUpdate(feature.getIdentifier())).thenReturn(Optional.of(feature));
        when(contexts.findByIdentifierForUpdate(context.getIdentifier())).thenReturn(Optional.of(context));
        when(contexts.findInternalIdByIdentifierForUpdate(context.getIdentifier())).thenReturn(Optional.of(1L));
        FeatureContextCommandService command = associationCommand(features, services, contexts, mock(FeatureContextRelationRepository.class));

        assertThatThrownBy(() ->
            command.associate(context.getIdentifier(), feature.getIdentifier())
        ).isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldPersistFeatureCreatedWithActiveService() {
        FeatureRepository features = mock(FeatureRepository.class);
        MicroserviceRepository services = mock(MicroserviceRepository.class);
        ContextRepository contexts = mock(ContextRepository.class);
        Microservice microservice = new Microservice("audit-service", "Audit Service", null, LocalDateTime.now());
        when(services.existsByIdentifier(microservice.getIdentifier())).thenReturn(true);
        if (microservice.isActive()) {
            when(services.findActiveIdForReference(microservice.getIdentifier())).thenReturn(Optional.of(1L));
            when(services.getReferenceById(1L)).thenReturn(microservice);
        }
        when(features.saveAndFlush(any(Feature.class))).thenAnswer(invocation -> invocation.getArgument(0));
        FeatureCommandService command = featureCommand(features, services);

        command.create(new CreateFeatureInput(null, "audit", "Audit", null, microservice.getIdentifier(), "{}", false));

        verify(features).saveAndFlush(any(Feature.class));
    }

    private static FeatureCommandService featureCommand(FeatureRepository features, MicroserviceRepository microservices) {
        FeatureFinder finder =
            new FeatureFinder(features, microservices, mock(EntityManager.class));
        FeatureMapper mapper =
            new FeatureMapper(JsonMapper.builder().build());
        return new FeatureCommandService(features, finder,
            new FeatureValidator(features,
                mock(SchemaSettingsValidationAdapter.class), mock(FeatureSharingReferencePort.class)), mapper);
    }

    private static FeatureContextCommandService associationCommand(FeatureRepository features,
        MicroserviceRepository microservices, ContextRepository contexts, FeatureContextRelationRepository relations) {
        FeatureMapper mapper = new FeatureMapper(JsonMapper.builder().build());
        return new FeatureContextCommandService(relations, new ContextFinder(contexts, mock(EntityManager.class)),
            new FeatureFinder(features, microservices, mock(EntityManager.class)), mapper, new FeatureContextValidator(),
            new FeatureContextAssociationOperations(relations, features, mapper));
    }

    private static ContextCommandService contextCommand(ContextRepository contexts, FeatureContextRelationRepository relations) {
        return new ContextCommandService(contexts, new ContextFinder(contexts, mock(EntityManager.class)),
            new ContextValidator(contexts, relations));
    }

    private static MicroserviceCommandService microserviceCommand(MicroserviceRepository repository) {
        return new MicroserviceCommandService(
            repository,
            new MicroserviceFinder(
                repository, mock(EntityManager.class)),
            new MicroserviceValidator(
                repository,
                mock(SchemaSettingsValidationAdapter.class)),
            new MicroserviceMapper(
                JsonMapper.builder().build())
        );
    }
}
