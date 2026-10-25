package br.com.portalmanager.platform.workspace.unit.feature.servicecontext.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.feature.Feature;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.featuregroup.FeatureGroup;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.microservice.Microservice;
import br.com.portalmanager.platform.workspace.feature.servicecontext.repository.feature.FeatureRepository;
import br.com.portalmanager.platform.workspace.feature.servicecontext.repository.featuregroup.FeatureGroupRelationRepository;
import br.com.portalmanager.platform.workspace.feature.servicecontext.repository.featuregroup.FeatureGroupRepository;
import br.com.portalmanager.platform.workspace.feature.servicecontext.repository.microservice.MicroserviceRepository;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.mapping.feature.FeatureMapper;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.mapping.featuregroup.FeatureGroupMapper;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.mapping.microservice.MicroserviceMapper;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.feature.CreateFeatureInput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.featuregroup.CreateFeatureGroupInput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.microservice.CreateMicroserviceInput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.feature.FeatureCommandService;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.feature.FeatureFinder;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.featuregroup.FeatureGroupAssociationCommandService;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.featuregroup.FeatureGroupAssociationOperations;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.featuregroup.FeatureGroupCommandService;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.featuregroup.FeatureGroupFinder;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.microservice.MicroserviceCommandService;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.microservice.MicroserviceFinder;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.port.FeatureUsagePort;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.port.SchemaValidationPort;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.validation.feature.FeatureValidator;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.validation.featuregroup.FeatureGroupAssociationValidator;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.validation.featuregroup.FeatureGroupValidator;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.validation.microservice.MicroserviceValidator;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

class ServiceContextCommandServiceTest {

    @Test
    void shouldRejectVersionOnCreateThroughUseCase() {
        MicroserviceRepository services = mock(MicroserviceRepository.class);
        MicroserviceCommandService command = microserviceCommand(services);

        assertThatThrownBy(
                        () ->
                                command.create(
                                        new CreateMicroserviceInput(
                                                1L, "audit-service", "Audit Service", null, "{}")))
                .isInstanceOf(ValidationException.class);
        verifyNoInteractions(services);
    }

    @Test
    void shouldRejectContextVersionOnCreateBeforeQuerying() {
        FeatureGroupRepository groups = mock(FeatureGroupRepository.class);
        FeatureGroupCommandService command =
                contextCommand(groups, mock(FeatureGroupRelationRepository.class));

        assertThatThrownBy(
                        () ->
                                command.create(
                                        new CreateFeatureGroupInput(
                                                1L, "manager-account", "Manager Account", null)))
                .isInstanceOf(ValidationException.class);
        verify(groups, never()).saveAndFlush(any(FeatureGroup.class));
    }

    @Test
    void shouldRejectInactiveServiceWhenCreatingFeature() {
        FeatureRepository features = mock(FeatureRepository.class);
        MicroserviceRepository services = mock(MicroserviceRepository.class);
        FeatureGroupRepository groups = mock(FeatureGroupRepository.class);
        Microservice microservice =
                new Microservice("audit-service", "Audit Service", null, LocalDateTime.now());
        microservice.inactivate(LocalDateTime.now());
        when(services.existsByIdentifier(microservice.getIdentifier())).thenReturn(true);
        FeatureCommandService command = featureCommand(features, services);

        assertThatThrownBy(
                        () ->
                                command.create(
                                        new CreateFeatureInput(
                                                null,
                                                "audit",
                                                "Audit",
                                                null,
                                                microservice.getIdentifier(),
                                                "{}",
                                                false)))
                .isInstanceOf(ValidationException.class);
        verify(features, never()).saveAndFlush(any(Feature.class));
    }

    @Test
    void shouldRejectFeatureActivationWhenServiceIsInactive() {
        FeatureRepository features = mock(FeatureRepository.class);
        MicroserviceRepository services = mock(MicroserviceRepository.class);
        FeatureGroupRepository groups = mock(FeatureGroupRepository.class);
        Microservice microservice =
                new Microservice("audit-service", "Audit Service", null, LocalDateTime.now());
        Feature feature =
                new Feature("audit", "Audit", null, microservice, "{}", LocalDateTime.now());
        feature.inactivate(LocalDateTime.now());
        microservice.inactivate(LocalDateTime.now());
        when(features.findMicroserviceIdentifierByIdentifier(feature.getIdentifier()))
                .thenReturn(Optional.of(microservice.getIdentifier()));
        when(services.existsByIdentifier(microservice.getIdentifier())).thenReturn(true);
        when(services.existsByIdentifier(microservice.getIdentifier())).thenReturn(true);
        FeatureCommandService command = featureCommand(features, services);

        assertThatThrownBy(() -> command.activate(feature.getIdentifier()))
                .isInstanceOf(ValidationException.class);
        verify(features, never()).saveAndFlush(any(Feature.class));
    }

    @Test
    void shouldRejectDuplicateMicroserviceCode() {
        MicroserviceRepository repository = mock(MicroserviceRepository.class);
        when(repository.existsByCode("audit-service")).thenReturn(true);
        MicroserviceCommandService command = microserviceCommand(repository);

        assertThatThrownBy(
                        () ->
                                command.create(
                                        new CreateMicroserviceInput(
                                                null,
                                                "audit-service",
                                                "Audit Service",
                                                null,
                                                "{}")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldRejectDuplicateServiceName() {
        MicroserviceRepository repository = mock(MicroserviceRepository.class);
        when(repository.existsByName("Audit Service")).thenReturn(true);
        MicroserviceCommandService command = microserviceCommand(repository);

        assertThatThrownBy(
                        () ->
                                command.create(
                                        new CreateMicroserviceInput(
                                                null,
                                                "audit-service",
                                                "Audit Service",
                                                null,
                                                "{}")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldRejectDuplicateFeatureName() {
        FeatureRepository features = mock(FeatureRepository.class);
        MicroserviceRepository services = mock(MicroserviceRepository.class);
        FeatureGroupRepository groups = mock(FeatureGroupRepository.class);
        when(features.existsByName("Audit")).thenReturn(true);
        FeatureCommandService command = featureCommand(features, services);

        assertThatThrownBy(
                        () ->
                                command.create(
                                        new CreateFeatureInput(
                                                null,
                                                "audit",
                                                "Audit",
                                                null,
                                                "microservice-id",
                                                "{}",
                                                false)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldRejectDuplicateFeatureGroupName() {
        FeatureGroupRepository groups = mock(FeatureGroupRepository.class);
        when(groups.existsByName("Manager Account")).thenReturn(true);
        FeatureGroupCommandService command =
                contextCommand(groups, mock(FeatureGroupRelationRepository.class));

        assertThatThrownBy(
                        () ->
                                command.create(
                                        new CreateFeatureGroupInput(
                                                "manager-account", "Manager Account", null)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldRejectGroupPurgeWhileItOwnsFeatures() {
        FeatureGroupRepository groups = mock(FeatureGroupRepository.class);
        FeatureGroup group =
                new FeatureGroup("manager-account", "Manager Account", null, LocalDateTime.now());
        FeatureGroupRelationRepository relations = mock(FeatureGroupRelationRepository.class);
        group.inactivate(LocalDateTime.now());
        when(relations.findFirstByFeatureGroupId(group.getId()))
                .thenReturn(
                        Optional.of(
                                mock(FeatureGroupRelationRepository.RelationIdReference.class)));

        when(groups.findByIdentifierForUpdate(group.getIdentifier()))
                .thenReturn(Optional.of(group));
        when(groups.findInternalIdByIdentifierForUpdate(group.getIdentifier()))
                .thenReturn(Optional.of(1L));
        FeatureGroupCommandService command = contextCommand(groups, relations);

        assertThatThrownBy(() -> command.purge(group.getIdentifier()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldRejectInactiveGroupAssociation() {
        FeatureRepository features = mock(FeatureRepository.class);
        MicroserviceRepository services = mock(MicroserviceRepository.class);
        FeatureGroupRepository groups = mock(FeatureGroupRepository.class);
        Microservice microservice =
                new Microservice("audit-service", "Audit Service", null, LocalDateTime.now());
        Feature feature =
                new Feature("audit", "Audit", null, microservice, "{}", LocalDateTime.now());
        FeatureGroup group =
                new FeatureGroup("administration", "administration", null, LocalDateTime.now());
        group.inactivate(LocalDateTime.now());

        when(features.findByIdentifierForUpdate(feature.getIdentifier()))
                .thenReturn(Optional.of(feature));
        when(groups.findByIdentifierForUpdate(group.getIdentifier()))
                .thenReturn(Optional.of(group));
        when(groups.findInternalIdByIdentifierForUpdate(group.getIdentifier()))
                .thenReturn(Optional.of(1L));
        FeatureGroupAssociationCommandService command =
                associationCommand(
                        features, services, groups, mock(FeatureGroupRelationRepository.class));

        assertThatThrownBy(() -> command.associate(group.getIdentifier(), feature.getIdentifier()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldPersistFeatureCreatedWithActiveService() {
        FeatureRepository features = mock(FeatureRepository.class);
        MicroserviceRepository services = mock(MicroserviceRepository.class);
        FeatureGroupRepository groups = mock(FeatureGroupRepository.class);
        Microservice microservice =
                new Microservice("audit-service", "Audit Service", null, LocalDateTime.now());
        when(services.existsByIdentifier(microservice.getIdentifier())).thenReturn(true);
        if (microservice.isActive()) {
            when(services.findActiveIdForReference(microservice.getIdentifier()))
                    .thenReturn(Optional.of(1L));
            when(services.getReferenceById(1L)).thenReturn(microservice);
        }
        when(features.saveAndFlush(any(Feature.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        FeatureCommandService command = featureCommand(features, services);

        command.create(
                new CreateFeatureInput(
                        null, "audit", "Audit", null, microservice.getIdentifier(), "{}", false));

        verify(features).saveAndFlush(any(Feature.class));
    }

    private static FeatureCommandService featureCommand(
            FeatureRepository features, MicroserviceRepository microservices) {
        FeatureFinder finder =
                new FeatureFinder(features, microservices, mock(EntityManager.class));
        FeatureMapper mapper = new FeatureMapper(JsonMapper.builder().build());
        return new FeatureCommandService(
                features,
                finder,
                new FeatureValidator(
                        features, mock(SchemaValidationPort.class), mock(FeatureUsagePort.class)),
                mapper);
    }

    private static FeatureGroupAssociationCommandService associationCommand(
            FeatureRepository features,
            MicroserviceRepository microservices,
            FeatureGroupRepository groups,
            FeatureGroupRelationRepository relations) {
        FeatureMapper mapper = new FeatureMapper(JsonMapper.builder().build());
        return new FeatureGroupAssociationCommandService(
                relations,
                groups,
                new FeatureGroupFinder(groups, mock(EntityManager.class)),
                new FeatureFinder(features, microservices, mock(EntityManager.class)),
                mapper,
                new FeatureGroupAssociationValidator(),
                new FeatureGroupAssociationOperations(relations, features, mapper));
    }

    private static FeatureGroupCommandService contextCommand(
            FeatureGroupRepository groups, FeatureGroupRelationRepository relations) {
        return new FeatureGroupCommandService(
                groups,
                new FeatureGroupFinder(groups, mock(EntityManager.class)),
                new FeatureGroupValidator(groups, relations),
                new FeatureGroupMapper());
    }

    private static MicroserviceCommandService microserviceCommand(
            MicroserviceRepository repository) {
        return new MicroserviceCommandService(
                repository,
                new MicroserviceFinder(repository, mock(EntityManager.class)),
                new MicroserviceValidator(repository, mock(SchemaValidationPort.class), List.of()),
                new MicroserviceMapper(JsonMapper.builder().build()));
    }
}
