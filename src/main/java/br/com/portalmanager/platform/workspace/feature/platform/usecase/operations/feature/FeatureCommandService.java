package br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.feature;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.feature.platform.domain.PlatformMessageKeys;
import br.com.portalmanager.platform.workspace.feature.platform.domain.feature.Feature;
import br.com.portalmanager.platform.workspace.feature.platform.domain.microservice.Microservice;
import br.com.portalmanager.platform.workspace.feature.platform.repository.feature.FeatureRepository;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.feature.CreateFeatureInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.feature.FeatureOutput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.feature.UpdateFeatureInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.validation.feature.FeatureValidator;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeatureCommandService {
    private final FeatureRepository repository;
    private final FeatureFinder finder;
    private final FeatureValidator validator;
    private final FeatureMapper mapper;

    public FeatureCommandService(FeatureRepository repository, FeatureFinder finder,
        FeatureValidator validator, FeatureMapper mapper) {
        this.repository = repository;
        this.finder = finder;
        this.validator = validator;
        this.mapper = mapper;
    }

    @Transactional
    @Auditable(action = AuditAction.CREATE, event = "FEATURE_CREATED", resourceType = "FEATURE")
    public FeatureOutput create(CreateFeatureInput input) {
        validator.validateForCreate(input);
        Microservice microservice = finder.findActiveMicroserviceForReference(input.microserviceIdentifier());
        Feature feature = new Feature(input.code(), mapper.toDetails(input), microservice, LocalDateTime.now());
        return mapper.toOutput(repository.saveAndFlush(feature));
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE, event = "FEATURE_UPDATED", resourceType = "FEATURE")
    public FeatureOutput update(String featureIdentifier, UpdateFeatureInput input) {
        if (input == null) {
            throw new ValidationException(PlatformMessageKeys.REQUEST_REQUIRED);
        }
        Microservice microservice = finder.findActiveMicroserviceForReference(input.microserviceIdentifier());
        Feature feature = finder.findForUpdate(featureIdentifier);
        feature.requireActive();
        validator.validateForUpdate(feature, input);
        feature.update(mapper.toDetails(input), microservice, LocalDateTime.now());
        repository.flush();
        return mapper.toOutput(feature);
    }

    @Transactional
    @Auditable(action = AuditAction.ACTIVATE, event = "FEATURE_ACTIVATED", resourceType = "FEATURE")
    public FeatureOutput activate(String featureIdentifier) {
        Feature feature = finder.findForUpdateWithActiveMicroservice(featureIdentifier);
        feature.activate(LocalDateTime.now());
        repository.flush();
        return mapper.toOutput(feature);
    }

    @Transactional
    @Auditable(action = AuditAction.DEACTIVATE, event = "FEATURE_DEACTIVATED", resourceType = "FEATURE")
    public FeatureOutput inactivate(String featureIdentifier) {
        Feature feature = finder.findForUpdate(featureIdentifier);
        feature.inactivate(LocalDateTime.now());
        repository.flush();
        return mapper.toOutput(feature);
    }
}
