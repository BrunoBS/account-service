package br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.feature;

import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.mapping.feature.FeatureMapper;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.ServiceContextMessageKeys;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.feature.Feature;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.microservice.Microservice;
import br.com.portalmanager.platform.workspace.feature.servicecontext.repository.feature.FeatureRepository;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.feature.CreateFeatureInput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.feature.FeatureOutput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.feature.UpdateFeatureInput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.validation.feature.FeatureValidator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class FeatureCommandService {
    private final FeatureRepository repository;
    private final FeatureFinder finder;
    private final FeatureValidator validator;
    private final FeatureMapper mapper;

    public FeatureCommandService(
            FeatureRepository repository,
            FeatureFinder finder,
            FeatureValidator validator,
            FeatureMapper mapper) {
        this.repository = repository;
        this.finder = finder;
        this.validator = validator;
        this.mapper = mapper;
    }

    @Transactional
    @Auditable(action = AuditAction.CREATE, event = "FEATURE_CREATED", resourceType = "FEATURE")
    public FeatureOutput create(CreateFeatureInput input) {
        validator.validateForCreate(input);
        Microservice microservice =
                finder.findActiveMicroserviceForReference(input.microserviceIdentifier());
        Feature feature = mapper.toDomain(input, microservice, LocalDateTime.now());
        return mapper.toOutput(repository.saveAndFlush(feature));
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE, event = "FEATURE_UPDATED", resourceType = "FEATURE")
    public FeatureOutput update(String featureIdentifier, UpdateFeatureInput input) {
        if (input == null) {
            throw new ValidationException(ServiceContextMessageKeys.REQUEST_REQUIRED);
        }
        Microservice microservice =
                finder.findActiveMicroserviceForReference(input.microserviceIdentifier());
        Feature feature = finder.findForUpdate(featureIdentifier);
        feature.requireActive();
        validator.validateForUpdate(feature, input);
        feature.update(mapper.toDetails(input), microservice, LocalDateTime.now());
        return mapper.toOutput(repository.saveAndFlush(feature));
    }

    @Transactional
    @Auditable(action = AuditAction.ACTIVATE, event = "FEATURE_ACTIVATED", resourceType = "FEATURE")
    public FeatureOutput activate(String featureIdentifier) {
        Feature feature = finder.findForUpdateWithActiveMicroservice(featureIdentifier);
        feature.activate(LocalDateTime.now());
        return mapper.toOutput(repository.saveAndFlush(feature));
    }

    @Transactional
    @Auditable(
            action = AuditAction.DEACTIVATE,
            event = "FEATURE_DEACTIVATED",
            resourceType = "FEATURE")
    public FeatureOutput inactivate(String featureIdentifier) {
        Feature feature = finder.findForUpdate(featureIdentifier);
        feature.inactivate(LocalDateTime.now());
        return mapper.toOutput(repository.saveAndFlush(feature));
    }
}
